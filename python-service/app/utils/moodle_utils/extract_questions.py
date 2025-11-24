import xml.etree.ElementTree as ET
import io
import re
from urllib.parse import unquote, unquote_plus
from app.models.ingestion_retrieval_models import Images, ImageChunk
from app.models.moodle_models.moodle_question import MoodleQuestion, MoodleQuestions

def extract_questions_from_moodle_xml(file_bytes: bytes) -> MoodleQuestions:
    """Extract Moodle cloze questions, including only images referenced in the question HTML.

    The Moodle XML contains <question type="cloze"> entries. Each has a <questiontext> with a <text>
    element holding HTML (often wrapped in CDATA) and zero or more <file> elements providing base64
    image data. Some <file> elements may not actually be referenced by <img src="@@PLUGINFILE@@/filename">.
    This function filters out such unreferenced images.
    """
    xml_content = io.BytesIO(file_bytes)
    tree = ET.parse(xml_content)
    root = tree.getroot()
    questions = MoodleQuestions(chunks=[])

    for child in root:
        # Only process cloze questions
        if child.tag == 'question' and child.attrib.get('type') == 'cloze':
            for grandchild in child:
                if grandchild.tag == 'questiontext':
                    # Collect children first so we can inspect text before deciding on images
                    qt_children = list(grandchild)
                    question = MoodleQuestion(question_text="", question_images=Images(chunks=[]))

                    # Get the HTML question text
                    text_node = next((c for c in qt_children if c.tag == 'text'), None)
                    if text_node is not None:
                        question.question_text = text_node.text or ""

                    # Build a map of available file attachments: name -> base64
                    file_map = {}
                    for c in qt_children:
                        if c.tag == 'file':
                            name_attr = c.attrib.get('name')
                            if name_attr:
                                file_map[name_attr] = c.text or ""

                    # Find referenced filenames in the order they appear in the HTML
                    referenced_in_text = re.findall(r'@@PLUGINFILE@@/([^"<>]+)', question.question_text)

                    # Append images in text order, matching raw and decoded filename variants
                    added = set()
                    for ref in referenced_in_text:
                        candidates = [ref, unquote(ref), unquote_plus(ref)]
                        for cand in candidates:
                            if cand in file_map and cand not in added:
                                question.question_images.chunks.append(ImageChunk(base64=file_map[cand]))
                                added.add(cand)
                                break
                    questions.chunks.append(question)
    return questions
