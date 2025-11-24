from typing import Any, List
from langchain_core.prompts.chat import ChatPromptTemplate
from app.models.ingestion_retrieval_models import Images
from app.models.enums import MoodlePromptBuilderType

prompt_extract_questions =  """
You are an AI that extracts structured data from Moodle XML question text.

INPUT:
You will receive HTML or CDATA Moodle question text that may include text, images, lists, and answers in Moodle format such as:
{{1:MC:%%100%%Correct~%%0%%Wrong}}
{{1:NUMERICAL:=75}}
and similar.

TASK:
1. Extract the given details and guidelines of the question and put it as a separate JSON object.
2. Parse the input and extract each exam question.
3. Extract the correct answer based ONLY on the Moodle answer encoding (e.g., %%100%% or :=).
4. Ignore formatting tags, images, metadata, and unrelated text.
5. Extract what type of question it is. There are 2 types: normal and table questions:
    - Normal questions are standard text-based questions.
    - Table questions contain multiple sub-questions organized in a table format.
6. Only categorize as 'table' if the question is explicitly structured as a table in HTML format. If not, then categorize as 'normal'. 
7. Return the output as a JSON object using the following structure:
{{
    "type": "<'normal' or 'table'>" 
    "guidelines": "<any guidelines or instructions associated with the question, if present>"
    "questions":
   [
        {{
            "question": "<cleaned question text>",
            "correct_answer": "<the correct answer as text>"
        }}
   ]
}}

RULES:

* The question text should be clean and human-readable.
* The correct answer must be extracted from the Moodle format, not inferred.
* Do NOT include explanations in the final output.
* If multiple blanks appear in one question, separate them into multiple question/answer objects.

OUTPUT:
Only return the JSON object in a JSON blob. No comments, no surrounding text.

Moodle XML Question Text:
'{moodle_question_text}'
"""

prompt_moodle_answer_question: List[dict[str, Any]] = [
    {
        "type": "text",
        "text": """
        **TASK:**
        The answers to the solved Moodle exam are provided from a professor. I'm a student trying to understand how to arrive at each answer.
        The answer to a solved question is directly after the '=' symbol after. EACH ANSWER IS CORRECT.
        Example: {{1:NUMERICAL:=75}} means the correct answer is 75, {{1:MCS:=B~C~A}} means the correct answer is B.
        Using the provided related material, explain step-by-step how to arrive at each answer WHICH IS ALREADY PROVIDED AND IS CORRECT.
        If it's calculation based, show the calculations needed to be done to arrive at the ALREADY PROVIDED CORRECT ANSWER.
        TELL HOW FOR EACH QUESTION TO ARRIVE AT THE ANSWER, FOR EXAMPLE:
        - Within the exam, there's an input field with '{{1:NUMERICAL:=75}}'.
        - You'd say 'The correct answer for that question is 75 because ...' and then provide reasoning based on the related material.
        - Do this for each question in the exam.
        
        **MOODLE QUIZ SOLVED EXAM:**
        {question}
        
        **RELATED MATERIAL:**
        {results_content}
        """
    },
]

prompt_answer_single_question: List[dict[str, Any]] = [
            {
                "type": "text",
                "text" : 
                """You are an Exam Question Answering Assistant.
                TASK:
                1. Use the provided guidelines: '{guidelines}'.
                2. Consider the question: '{question}'.
                3. The correct answer (already determined) is: '{answer}'. Do NOT restate that it is correct.
                4. Derive the answer ONLY from the supplied course material and any images if present.
                IMAGE USAGE:
                - Refer to images only when necessary; describe observable features succinctly (e.g., "Image 1 shows ...").
                - Do not speculate beyond what is visible.
                STYLE RULES:
                - No praise, evaluation, or conversational fillers.
                - Impersonal tone (avoid 'you', 'your').
                - No meta commentary about the explanation quality.
                - No external knowledge beyond provided context.
                - Do not repeat these instructions.
                OUTPUT FORMAT:
                Provide a numbered sequence of concise reasoning steps (1., 2., 3., ...). Each step may cite short quoted fragments from the context in double quotes when supporting a claim. Return ONLY the steps: no heading, preface, or conclusion.
                CONTEXT (cite needed fragments): '{results_content}'
                """,
            },
]

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