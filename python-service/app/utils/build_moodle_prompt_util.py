from typing import Any, List
from langchain_core.prompts.chat import ChatPromptTemplate
from app.models.ingestion_retrieval_models import Images
from app.models.enums import MoodlePromptBuilderType
from app.prompts.prompt_moodle_answer_question import prompt_moodle_answer_question, prompt_answer_single_question
from app.prompts.prompt_moodle_generate_quiz import prompt_moodle_generate_quiz

def build_moodle_prompt(images: Images, mode: MoodlePromptBuilderType) -> ChatPromptTemplate:
    """Build a multimodal prompt template.

    For each image chunk we add a placeholder referencing an input variable
    (image_0, image_1, ...). At invocation time those variables should be
    supplied in the input_data dict, e.g. input_data["image_0"] = <base64 string>.
    LangChain will then substitute them into the image_url.
    """
    if mode == MoodlePromptBuilderType.ANSWER_ALL_QUESTIONS:
        basic_prompt = prompt_moodle_answer_question.copy()
        print(basic_prompt)
    
    elif mode == MoodlePromptBuilderType.ANSWER_SINGLE_QUESTION:
        basic_prompt = prompt_answer_single_question.copy()
    
    elif mode == MoodlePromptBuilderType.GENERATE_QUIZ:
        basic_prompt = prompt_moodle_generate_quiz.copy()
        
    if images and images.chunks:
        for i, _ in enumerate(images.chunks):
            # Use a template variable inside the URL so the base64 can be
            # injected at runtime via input_data (image_i keys).
            basic_prompt.append(  # type: ignore[arg-type]
                {
                    "type": "image_url",
                    "image_url": {"url": f"data:image/jpeg;base64,{{image_{i}}}"},
                }
            )
    return ChatPromptTemplate.from_messages([("user", basic_prompt)])