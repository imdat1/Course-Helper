from fastapi import FastAPI, APIRouter, UploadFile, Form, HTTPException, Response
from pydantic import ValidationError
from celery.result import AsyncResult
from app.models.request import AIRequest
from app.models.video import Video
from app.services.flash_card_service import create_flash_cards_service, evaluate_flash_card_answer
from celery_worker import (
    process_ai_request_task,
    upload_file_task,
    create_video_uri_using_gemini_task,
    parse_summarize_ingest_pdf_task,
    create_flash_cards_task,
    delete_file_points_task,
    delete_collection_task,
    process_xml_file,
    export_moodle_quiz
    )  
import uuid
from app.models.request import UriRequest
from app.models.enums import UriProvider, FileTypeFastAPI
# from app.utils.upload_video_dependency_util import video_json_dependency
from typing import Optional
from app.models.flash_cards import FlashCardList
from app.models.answer_evaluation import AnswerEvaluationRequest, AnswerEvaluationResult
import json
from app.models.moodle_models.dto_moodle_question import DTOMoodleQuestion

router = APIRouter()

@router.post("/process-ai")
async def process_ai_request(request: AIRequest):
    """API endpoint to process AI requests asynchronously"""
    task = process_ai_request_task.apply_async(args=[request.model_dump()])  # Call apply_async here
    return {"task_id": task.id, "status": "PENDING"}

@router.get("/task-status/{task_id}")
async def get_task_status(task_id: str):
    """Check the status of a Celery task"""
    task_result = AsyncResult(task_id)  # Directly check status without a Celery task
    return {"task_id": task_id, "status": task_result.status, "result": task_result.result}

@router.post("/upload/")
async def upload_file(video: Optional[str] = Form(None), file: Optional[UploadFile] = None, api_key: Optional[str] = None, collection_name: Optional[str] = Form(None)):
    """
    Endpoint to handle file uploads
    
    Args:

        api_key (Optional[str]): API key for summarisation and ingestion in Qdrant vector database, and generation of Flash Cards. 

        file (UploadFile): Uploaded file object

        video (Optional[Video]): Video object SPECIFICALLY WILL ONLY BE USED TO GENERATE FLASH CARDS!! SO ONLY EITHER UPLOAD FILE OR ADD VIDEO, NOT BOTH!
        In SwaggerUI at localhost:8000/docs, this will be shown as a string upload, but add a valid Video object in JSON format.
        Example:
        {
            "type": "YOUTUBE_VIDEO",
            "uri": "https://www.youtube.com/watch?v=fWjsdhR3z3c"
        }
        
    Returns:
        dict: File upload information
    """
    if file is not None:
        file_id = str(uuid.uuid4())  # Create a unique ID for the file
        
        # Read file content into memory
        file_content = await file.read()
        print(file.content_type)
        
        # Trigger Celery task with file content
        task = upload_file_task.apply_async(args=[file_id, file.filename, file_content])

        return_obj = {"filename": file.filename, "file_id": file_id, "task_id": task.id}
        
        print(FileTypeFastAPI.XML)
        
        if (file.content_type in FileTypeFastAPI.XML and file.filename.lower().endswith(".xml")):
            target_collection = collection_name if collection_name else file_id
            process_xml_task = process_xml_file.apply_async(args=[file_id, file.filename, target_collection, file_content, api_key])
            return_obj["process_xml_task_id"] = process_xml_task.id

        if ((file.content_type==FileTypeFastAPI.PDF and file.filename.lower().endswith(".pdf"))
            or (file.content_type == FileTypeFastAPI.WORD and file.filename.lower().endswith(".docx"))
            or (file.content_type == FileTypeFastAPI.POWERPOINT and file.filename.lower().endswith(".pptx"))):
            # If a course-level collection_name is provided, ingest into that; else default to file_id
            target_collection = collection_name if collection_name else file_id
            parse_summarise_ingest_task = parse_summarize_ingest_pdf_task.apply_async(args=[file_id, file.filename, file_content, api_key, file.content_type, target_collection])
            return_obj["parse_summarise_ingest_task_id"] = parse_summarise_ingest_task.id
        
        return return_obj
    
    elif video is not None:
        # Flash cards creation logic (replace with your function)
        try:
            video_obj = Video(**json.loads(video))  # Parse JSON manually
        except json.JSONDecodeError:
            raise HTTPException(status_code=400, detail="Invalid JSON in 'video' field")
        
        flash_cards = create_flash_cards_service(api_key=api_key, video=video_obj)
        return_obj = {"video": video_obj if isinstance(video,dict) else video_obj.model_dump(),
                      "flash_cards": flash_cards.to_json_serializable() if isinstance(flash_cards, FlashCardList) else flash_cards}
        return return_obj
    else:
        return_obj = {"error": "No file or video provided."}
        return return_obj
    

## Removed redundant /upload_file endpoint; /upload/ already handles XML processing.


@router.post("/get_video_uri")
async def get_video_uri_route(request: UriRequest):
    if request.uri_provider == UriProvider.GEMINI_URI_PROVIDER:
        if request.api_key is None:
            raise Exception("An API key is needed for the Gemini URI provider.")
        task = create_video_uri_using_gemini_task.apply_async(args=[request.model_dump()])
    return {"task_id": task.id, "status": "PENDING"}

@router.post("/create_flash_cards")
async def create_flash_cards_route(collection_name: Optional[str] = None, api_key: Optional[str] = None, video: Optional[Video] = None):
    """
    Endpoint to handle creation of flash cards. KEEP IN MIND: 5 flash cards are created in question/answer pairs.
    
    KEEP IN MIND: Flash cards are also created when uploading a file in the parse-summarise-ingest task.

    Args:

        video (Optional[Video]): Video object SPECIFICALLY WILL ONLY BE USED TO GENERATE FLASH CARDS!! SO ONLY EITHER UPLOAD COLLECTION NAME OR ADD VIDEO, NOT BOTH!

        collection_name (str): the Qdrant collection name

        api_key (str): Gemini API key since we're using their models

    Returns:
        dict: flash_cards  
    """
    task = create_flash_cards_task.apply_async(args=[api_key, collection_name, video.model_dump() if video else None])
    return {"task_id": task.id, "status": "PENDING"}

@router.post("/delete_file_from_collection")
async def delete_file_from_collection(collection_name: str = Form(...), file_id: str = Form(...)):
    """Enqueue deletion of all points in a collection for a specific uploaded file (by file_id)."""
    try:
        task = delete_file_points_task.apply_async(args=[collection_name, file_id])
        return {"task_id": task.id, "status": "PENDING"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Deletion enqueue failed: {str(e)}")


@router.post("/delete_collection")
async def delete_collection(collection_name: str = Form(...)):
    """Enqueue deletion of an entire Qdrant collection by name."""
    try:
        task = delete_collection_task.apply_async(args=[collection_name])
        return {"task_id": task.id, "status": "PENDING"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Collection deletion enqueue failed: {str(e)}")


@router.post("/evaluate_flash_card_answer", response_model=AnswerEvaluationResult)
async def evaluate_flash_card_answer_route(request: AnswerEvaluationRequest):
    """
    Evaluate a user's answer to a flash card using an LLM and optionally
    attach the source file name from Qdrant (if collection_name is provided).

    Body: AnswerEvaluationRequest
    Returns: AnswerEvaluationResult
    """
    try:
        result = evaluate_flash_card_answer(
            api_key=request.api_key,
            question=request.question,
            expected_answer=request.expected_answer,
            user_answer=request.user_answer,
            collection_name=request.collection_name,
        )
        return AnswerEvaluationResult(**result)
    except ValidationError as ve:
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Evaluation failed: {str(e)}")


@router.post("/export_quiz")
async def export_quiz_route(payload: dict):
    """
    Enqueue export of a Moodle quiz XML from provided questions DTO dicts.

    Expects JSON body:
    {
      "questions": [ { ... DTOMoodleQuestion dict ... }, ... ],
      "collection_name": "course_123_materials",
      "api_key": "<gemini_api_key>"
    }
    Returns: { "task_id": str, "status": "PENDING" }
    """
    try:
        questions = payload.get("questions", [])
        collection_name = payload.get("collection_name")
        api_key = payload.get("api_key")
        if not isinstance(questions, list) or not collection_name or not api_key:
            raise HTTPException(status_code=400, detail="questions[], collection_name and api_key are required")

        # We pass the list of dicts directly; the Celery task will parse into DTOMoodleQuestion
        task = export_moodle_quiz.apply_async(args=[questions, collection_name, api_key])
        return {"task_id": task.id, "status": "PENDING"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to enqueue export: {str(e)}")


@router.get("/export_quiz/{task_id}/download")
async def download_exported_quiz(task_id: str):
    """
    Download the generated Moodle XML once the export task is SUCCESS.
    Returns application/xml with Content-Disposition for download.
    """
    task_result = AsyncResult(task_id)
    if task_result.status != "SUCCESS":
        return {"task_id": task_id, "status": task_result.status}
    result_bytes = task_result.result
    if not isinstance(result_bytes, (bytes, bytearray)):
        raise HTTPException(status_code=500, detail="Task did not return file bytes")
    headers = {"Content-Disposition": f"attachment; filename=moodle_quiz_export_{task_id}.xml"}
    return Response(content=result_bytes, media_type="application/xml", headers=headers)