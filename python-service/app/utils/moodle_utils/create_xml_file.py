import xml.dom.minidom as minidom

def create_moodle_xml(questions_list_dict, output_file="output.xml"):
    # Create XML document
    doc = minidom.Document()

    quiz = doc.createElement("quiz")
    doc.appendChild(quiz)

    for question_dict in questions_list_dict:
        # <question type="cloze">
        question = doc.createElement("question")
        question.setAttribute("type", "cloze")
        quiz.appendChild(question)

        # <questiontext format="html">
        questiontext = doc.createElement("questiontext")
        questiontext.setAttribute("format", "html")
        question.appendChild(questiontext)

        # <text> <![CDATA[ ... ]]></text>
        text_node = doc.createElement("text")
        question_creator = f"""
        <p>This is an AI generated question. Answers can be wrong or miscalculated.
        Please review before use.
        <p>Now here is the question:</p>
        {question_dict.get('question_html', '')}
        """
        cdata = doc.createCDATASection(question_creator)   # <-- REAL CDATA, no escaping
        text_node.appendChild(cdata)
        questiontext.appendChild(text_node)

    # Save
    with open(output_file, "w", encoding="utf-8") as f:
        doc.writexml(f, indent="", addindent="  ", newl="\n", encoding="UTF-8")