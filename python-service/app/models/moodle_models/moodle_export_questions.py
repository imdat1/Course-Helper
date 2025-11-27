from pydantic import BaseModel, Field

class MoodleExportQuestions(BaseModel):
    questions_list_dict: list[dict[str, str]] = Field(default_factory=list, description="List of Moodle XML formatted questions.")
    
    