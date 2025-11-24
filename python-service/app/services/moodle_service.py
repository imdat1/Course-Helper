from app.utils.moodle_utils.extract_questions import extract_questions_from_moodle_xml
from app.models.moodle_models.moodle_question import MoodleQuestions
from app.services.summarisation_service import Summarisation
from langchain_core.language_models.chat_models import BaseChatModel
from app.prompts.prompt_moodle_answer_question import build_moodle_prompt, prompt_extract_questions
from langchain_core.output_parsers import StrOutputParser
from langchain_core.embeddings.embeddings import Embeddings
from qdrant_client import QdrantClient
from app.core.config_settings import get_settings
from app.utils.qdrant_vector_database_util import get_closest_points_from_qdrant
from app.models.enums import MoodlePromptBuilderType
from langchain_core.prompts.chat import ChatPromptTemplate
from langchain_core.output_parsers import JsonOutputParser

settings = get_settings()

class MoodleService():
    model: BaseChatModel = None
    embedding_model: Embeddings = None
    summarisation_service: Summarisation = None
    questions: MoodleQuestions = None 
    client = QdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)

    def __init__(self, model: BaseChatModel, embedding_model: Embeddings, file_content: bytes) -> None:
        self.model = model
        self.embedding_model = embedding_model
        self.summarisation_service = Summarisation(model=model)
        self.questions = extract_questions_from_moodle_xml(file_content)
    
    def summarise_moodle_questions(self):
        self.questions = self.summarisation_service.summarise_chunks(
            container=self.questions,
            extract_input=lambda question: question.question_text, 
            summarization_chain=self.summarisation_service.prompt_moodle_question_summarisation_chain
        )
    
    def summarise_moodle_images(self):
        for question in self.questions.chunks:
            if question.question_images and question.question_images.chunks:
                summarized_images = self.summarisation_service.summarise_chunks(
                    container=question.question_images,
                    extract_input=lambda image_chunk: image_chunk.base64,  
                    summarization_chain=self.summarisation_service.summarize_images_chain
                )
                question.question_images = summarized_images

    def answer_moodle_questions(self, collection_name: str):
        for question in self.questions.chunks:
            chain = build_moodle_prompt(images=question.question_images, mode=MoodlePromptBuilderType.ANSWER_ALL_QUESTIONS) | self.model | StrOutputParser()
            results_content = get_closest_points_from_qdrant(
                client=self.client,
                collection_name=collection_name,
                embedded_query=self.embedding_model.embed_query(question.summary),
            )
            input_data = {
                "question": question.question_text,
                "results_content": results_content,
            }
            if question.question_images and question.question_images.chunks:
                for i, image in enumerate(question.question_images.chunks):
                    input_data[f"image_{i}"] = f"{image.base64}"
                
            question.ai_answer = chain.invoke(input_data)

    def extract_question_data(self):
        """Populate each question's extracted_question_data using the prompt_extract_questions template.

        Uses the already-configured chat model (self.model) and a JsonOutputParser to
        return structured guideline + question/answer information per Moodle question.
        If parsing fails for a question, its extracted_question_data is left as None.
        """

        prompt = ChatPromptTemplate.from_messages([("user", prompt_extract_questions)])
        chain = prompt | self.model | JsonOutputParser()
        for question in self.questions.chunks:
            inputs = {"moodle_question_text": question.question_text}
            try:
                result = chain.invoke(inputs)
                question.extracted_question_data = result
            except Exception:
                question.extracted_question_data = None
                
    def answer_moodle_questions_independently(self, collection_name: str):
        for item in self.questions.chunks:
            embedded_query = self.embedding_model.embed_query(item.summary)
            results_content = get_closest_points_from_qdrant(
                    client=self.client,
                    collection_name=collection_name,
                    embedded_query=embedded_query,
            ) 
            guidelines = item.extracted_question_data.get('guidelines', '')
            type = item.extracted_question_data.get('type', '')
            if type == 'table':
                prompt = build_moodle_prompt(
                    images=item.question_images, mode=MoodlePromptBuilderType.ANSWER_ALL_QUESTIONS
                )
                chain = prompt | self.model | StrOutputParser()
                input_data = {
                    "question": item.question_text,
                    "results_content": results_content,
                }
                if item.question_images and item.question_images.chunks:
                    for i, image in enumerate(item.question_images.chunks):
                        input_data[f"image_{i}"] = f"{image.base64}"
                    
                item.extracted_question_data['ai_reasoning'] = chain.invoke(input_data)
            elif type == 'normal':  
                for question in item.extracted_question_data.get('questions', []):
                    prompt = build_moodle_prompt(
                        images=item.question_images, mode=MoodlePromptBuilderType.ANSWER_SINGLE_QUESTION
                    )
                    
                    chain = prompt | self.model | StrOutputParser()
                    input_data = {
                        "guidelines": guidelines,
                        "question": question.get("question", ""),
                        "answer": question.get("correct_answer", ""),
                        "results_content": results_content,
                    }
                    if item.question_images and item.question_images.chunks:
                        for i, image in enumerate(item.question_images.chunks):
                            input_data[f"image_{i}"] = f"{image.base64}"
                        
                    question['ai_reasoning'] = chain.invoke(input_data)
            else:
                return  # Unknown type; skip