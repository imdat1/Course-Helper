from typing import Any
from pydantic import BaseModel, Field
from app.models.ingestion_retrieval_models import ChunkBase, Images, ImageChunk

class MoodleQuestion(ChunkBase):
    question_text: str = Field(..., description="The text of the question")
    ai_answer: str | None = Field(default=None, description="AI-generated answer to the question")
    question_images: Images = Field(default_factory=lambda: Images(chunks=[]), description="URL or path to the question image")
    answer_file: str | None = Field(default=None, description="The file associated with the question")
    extracted_question_data: dict[str, Any] | None = Field(
        default=None,
        description="Structured extraction of guidelines and question/answer pairs from raw Moodle XML text"
    )

class MoodleQuestions(BaseModel):
    chunks: list[MoodleQuestion]