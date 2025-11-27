prompt_moodle_generate_quiz = [
    ("user", 
        [
            {
                "type": "text",
                "text": """
You are a Moodle-XML quiz question generator.  
Your job: based on the provided quiz question, how to arrive at answers, and related material, produce **one new** Moodle XML quiz question in HTML format that matches the original question's type and difficulty but is unique.

INSTRUCTIONS (must follow exactly):
1. THE FIRST THING YOU MUST DO IS CREATE THE NEW GUIDELINES WITH VALUES IN MARKDOWN FORM.
Then generate the new questions and answers based on those guidelines.
Provide how to arrive at the answer for EACH QUESTION. Do NOT miss a question.
This includes any calculations, logic, or steps taken to arrive at the answer.
This should be **outside** of the final output Moodle XML HTML format question.
2. Lastly, **output** the new Moodle XML question text in HTML blob format in the language on which the quiz is based.
3. The new question must follow the same *type, difficulty, and formatting style* as the original, 
    BUT MUST NOT reuse the original content. 
    - Do NOT repeat the original diagrams, tables, figures, numeric values, dataset, or text.
    - Invent new values, new diagrams/tables, and new contextual details.
    - The question must be fully new and unique, not a rephrasing of the original.
4. **Answer formatting rules** (use exactly these inline answer formats inside the HTML):
- Multiple-choice (single correct): `{{1:MC:=%%100%%CorrectAnswer~%%0%%WrongAnswer1~%%0%%WrongAnswer2}}`
- Multiple-choice (single correct — alternative style that may appear): `{{1:MCS:=CorrectAnswer~WrongAnswer1~WrongAnswer2}}`
- Numerical: `{{1:NUMERICAL:=CorrectNumericAnswer}}`

OUTPUT EXAMPLE:
"
Guidelines for the new questions:
- <new guidelines here>

1. Question 1: Text and input fields for the question
2. Logic or calculations on how to arrive at the answer for Question 1: ...

1. Question 2: Text and input fields for the question
2. Logic or calculations on how to arrive at the answer for Question 2: ...

Final Output:
```html
<Moodle XML question in HTML format>
```
"

Now generate a single new Moodle XML question in HTML format using the inputs below.
**Remember: question generation and logic comes first, then output with the new question HTML blob comes second.**

Quiz question:
```
{quiz_question}
```

Explanations on how to reach each answer of the previous quiz question:
```
{explanations}
```

Related material:
```
{related_material}
```
                """
            }
        ]
    )
]

prompt_image_descriptor = [
    (
        "user",
        [
            {
                "type": "text",
                "text": """Describe the image and only provide HTML output format.
                If there's a table, try to represent it accurately in HTML format.
                Try to use ASCII explainatory art for diagrams and graphs.
                Don't include opinions, only provide the HTML element.
                This means only return the <table>, <div>, <svg> or other HTML elements.
                Include styling and color if relevant.
                Be specific about diagrams, graphs and tables, such as bar plots."""
            },
            {
                "type": "image_url",
                "image_url": {"url": "data:image/jpeg;base64,{image}"},
            },
        ],
    )
]

prompt_reasoning_test = [("user", [
    {
        "type": "text",
        "text": """For the following questions, explain how to reach each correct answer.
        This includes the reason why a question is correct based on related material or calculations.
        Your calculations MUST be aligned with the correct answers, if they're not then you're WRONG, re-examine yourself.
        Question formatting:
            - Multiple-choice (single correct): `{{1:MC:=%%100%%CorrectAnswer~%%0%%WrongAnswer1~%%0%%WrongAnswer2}}`
            - Multiple-choice (single correct — alternative style that may appear): `{{1:MCS:=CorrectAnswer~WrongAnswer1~WrongAnswer2}}`
            - Numerical: `{{1:NUMERICAL:=CorrectNumericAnswer}}`
        Each answer is absolutely totally CORRECT and don't assume else.
        Question:
        ```{quiz_question}```
        
        Related material:
        ```{related_material}```
        """
    }
])]
