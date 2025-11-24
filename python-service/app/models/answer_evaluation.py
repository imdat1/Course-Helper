from pydantic import BaseModel, Field
from typing import Optional, Dict, Any


class AnswerEvaluationRequest(BaseModel):
    question: str = Field(..., description="The flash card question")
    expected_answer: str = Field(..., description="The correct/expected answer")
    user_answer: str = Field(..., description="The user's answer to evaluate")
    api_key: str = Field(..., description="Gemini API key to run evaluation")
    collection_name: Optional[str] = Field(
        default=None,
        description="Qdrant collection to lookup source file name (optional)",
    )


class AnswerEvaluationResult(BaseModel):
    question: str
    expected_answer: str
    user_answer: str
    evaluation: Dict[str, Any]
    source_file_name: Optional[str] = None