from pydantic import BaseModel, Field, ConfigDict
from typing import Any, Optional
from app.models.moodle_models.moodle_question import MoodleQuestion
from app.models.ingestion_retrieval_models import ImageChunk


class DTOMoodleImage(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    image_base64: str = Field(..., alias="img_base64")
    ai_summary: Optional[str] = Field(default=None, alias="ai_summary")

    @classmethod
    def from_image_chunk(cls, chunk: ImageChunk) -> "DTOMoodleImage":
        return cls(img_base64=chunk.base64, ai_summary=chunk.summary)


class DTOMoodleQuestion(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    question_text: str
    question_summary: str | None = None
    question_images: list[DTOMoodleImage] = Field(default_factory=list)
    questions_parsed_and_answered: dict[str, Any] = Field(default_factory=dict)

    @classmethod
    def from_moodle_question(cls, mq: MoodleQuestion) -> "DTOMoodleQuestion":
        images = [DTOMoodleImage.from_image_chunk(c) for c in mq.question_images.chunks]
        return cls(
            question_text=mq.question_text,
            question_summary=mq.summary,
            questions_parsed_and_answered=mq.extracted_question_data or {},
            question_images=images,
        )

    def to_dict(self) -> dict:
        return self.model_dump(by_alias=True, exclude_none=True)