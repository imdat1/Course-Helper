import os
import xml.dom.minidom as minidom

def _sanitize_for_cdata(text: str) -> str:
    """Sanitize text for inclusion inside a CDATA section.

    CDATA cannot contain the literal sequence ']]>'. To preserve content while
    keeping a single CDATA node, split the sequence safely by closing and
    reopening CDATA at that point.
    """
    if text is None:
        return ""
    # Replace any occurrence of ']]>' with a safe split across CDATA boundaries
    # per XML best practice.
    return text.replace(
        "]]>",
        "]]]]]><![CDATA[>"
    )

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
        # Ensure CDATA-safe content (no raw ']]>' sequences)
        safe_text = _sanitize_for_cdata(question_creator)
        cdata = doc.createCDATASection(safe_text)   # real CDATA
        text_node.appendChild(cdata)
        questiontext.appendChild(text_node)

    # Ensure output directory exists, then save
    output_dir = os.path.dirname(output_file)
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)

    with open(output_file, "w", encoding="utf-8") as f:
        doc.writexml(f, indent="", addindent="  ", newl="\n", encoding="UTF-8")