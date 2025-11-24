from app.core.config_settings import get_settings
from app.models import flash_cards
from app.models.video import Video
from app.utils.qdrant_vector_database_util import get_random_data_from_qdrant
from app.prompts.prompt_flash_card_generation import prompt_flash_card_generation, prompt_flash_card_generation_from_video
import json
import re
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_google_genai.embeddings import GoogleGenerativeAIEmbeddings
from typing import Optional
from qdrant_client import QdrantClient
import time
from pydantic import SecretStr
from qdrant_client import models as qmodels

settings = get_settings()
qdrant_client = QdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)

def generate_questions_and_answers(collection_name: str, api_key: str) -> list[dict]:
    texts = get_random_data_from_qdrant(qdrant_client=qdrant_client, collection_name=collection_name)
    prompt = prompt_flash_card_generation(texts)
    model = ChatGoogleGenerativeAI(model = "gemini-2.0-flash", api_key=SecretStr(api_key))
    # Invoke with 60s wait-and-retry on resource exhaustion (same logic as summarisation service)
    while True:
        try:
            response = model.invoke([
                {"role": "system", "content": "You are an assistant."},
                {"role": "user", "content": prompt}
            ])
            break
        except Exception as e:
            if "ResourceExhausted: 429" in str(e) or "You exceeded your current quota" in str(e):
                print("Rate limit reached. Waiting 60 seconds before retrying... FLASH CARDS!")
                time.sleep(60)
                continue
            else:
                raise
    
    # Parse response
    pattern = r"```json\s*([\s\S]*?)\s*```"
    content_str = response.content if isinstance(response.content, str) else str(response.content)
    match = re.search(pattern, content_str)
    if match:
        json_str = match.group(1).strip()
        try:
            return json.loads(json_str)
        except json.JSONDecodeError as e:
            print(f"Error parsing JSON: {e}")
            return []
    else:
        print("No JSON code block found")
        return []

def generate_questions_and_answers_from_video(video: Video, api_key: str) -> list[dict]:
    prompt = prompt_flash_card_generation_from_video(video)
    model = ChatGoogleGenerativeAI(model = "gemini-2.0-flash", api_key=SecretStr(api_key))
    # Invoke with 60s wait-and-retry on resource exhaustion (same logic as summarisation service)
    while True:
        try:
            response = model.invoke(prompt)
            break
        except Exception as e:
            if "ResourceExhausted: 429" in str(e) or "You exceeded your current quota" in str(e):
                print("Rate limit reached. Waiting 60 seconds before retrying...FLASH CARDS!")
                time.sleep(60)
                continue
            else:
                raise
    
    # Parse response
    pattern = r"```json\s*([\s\S]*?)\s*```"
    content_str = response.content if isinstance(response.content, str) else str(response.content)
    match = re.search(pattern, content_str)
    if match:
        json_str = match.group(1).strip()
        try:
            return json.loads(json_str)
        except json.JSONDecodeError as e:
            print(f"Error parsing JSON: {e}")
            return []
    else:
        print("No JSON code block found")
        return []

def create_flash_cards_service(api_key: str,
                                collection_name: Optional[str] = None,
                                video: Optional[Video] = None,
                               ) -> flash_cards.FlashCardList:
    if collection_name is not None:
        qa_pairs = generate_questions_and_answers(collection_name, api_key)
    elif video is not None:
        qa_pairs = generate_questions_and_answers_from_video(video, api_key)
    else:
        raise ValueError("Either collection_name or video must be provided.")
    flash_card_list = []
    for qa in qa_pairs:
        flash_card = flash_cards.FlashCard(
            question=qa['question'],
            answer=qa['answer'],
        )
        flash_card_list.append(flash_card)

    flash_card_list_obj = flash_cards.FlashCardList(flash_cards=flash_card_list)
    return flash_card_list_obj


def _extract_json_code_block(text: str) -> Optional[dict]:
    pattern = r"```json\s*([\s\S]*?)\s*```"
    match = re.search(pattern, text)
    if match:
        json_str = match.group(1).strip()
        try:
            return json.loads(json_str)
        except json.JSONDecodeError:
            return None
    # try parse as raw JSON if no code fence present
    try:
        return json.loads(text)
    except Exception:
        return None


def _get_source_file_name_for_answer(
    *,
    api_key: str,
    collection_name: Optional[str],
    query_text: str,
) -> Optional[str]:
    if not collection_name:
        return None
    try:
        embedding_model = GoogleGenerativeAIEmbeddings(
            model="models/text-embedding-004",
            google_api_key=api_key,
            task_type="retrieval_query",
        )
        embedded_query = embedding_model.embed_query(query_text)
        results = qdrant_client.search(
            collection_name=collection_name,
            query_vector=embedded_query,
            limit=1,
            with_payload=True,
        )
        if results and results[0].payload:
            return results[0].payload.get("file_name")
        return None
    except Exception:
        return None


def evaluate_flash_card_answer(
    *,
    api_key: str,
    question: str,
    expected_answer: str,
    user_answer: str,
    collection_name: Optional[str] = None,
) -> dict:
    system_msg = "You are a strict but fair teacher. Evaluate student answers concisely."
    user_prompt = (
        "Evaluate the student's answer. Return a JSON object with keys: "
        "verdict (one of: Correct, Partially Correct, Incorrect), "
        "score (0-100 integer), "
        "feedback (brief actionable feedback).\n\n"
        f"Question: {question}\n"
        f"Expected answer: {expected_answer}\n"
        f"Student answer: {user_answer}\n\n"
        "Respond ONLY with JSON."
    )

    model = ChatGoogleGenerativeAI(model="gemini-2.0-flash", api_key=SecretStr(api_key))

    while True:
        try:
            resp = model.invoke([
                {"role": "system", "content": system_msg},
                {"role": "user", "content": user_prompt},
            ])
            break
        except Exception as e:
            if "ResourceExhausted: 429" in str(e) or "You exceeded your current quota" in str(e):
                print("Rate limit reached. Waiting 60 seconds before retrying... EVAL!")
                time.sleep(60)
                continue
            else:
                raise

    content_str = resp.content if isinstance(resp.content, str) else str(resp.content)
    parsed = _extract_json_code_block(content_str) or {}

    # Attach source file name using nearest context to expected answer (fallback to question)
    source_file = _get_source_file_name_for_answer(
        api_key=api_key,
        collection_name=collection_name,
        query_text=f"{question}\n\n{expected_answer}",
    )

    result = {
        "evaluation": parsed,
        "source_file_name": source_file,
        "question": question,
        "expected_answer": expected_answer,
        "user_answer": user_answer,
    }
    return result