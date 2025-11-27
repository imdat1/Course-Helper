import re

def replace_images_with_descriptions(html_text, descriptions):
    """
    Replaces each <img ...> tag in html_text with matching textual descriptions.
    'descriptions' must be a list of strings.
    """
    img_pattern = r'<img[^>]*>'  # match any <img ...>

    # Find all image tags
    images = re.findall(img_pattern, html_text)

    if len(descriptions) < len(images):
        raise ValueError("Not enough descriptions for number of <img> tags")

    # Replace each image with its description
    def replacement(match):
        # Pop first description from the list
        return descriptions.pop(0)

    new_text = re.sub(img_pattern, replacement, html_text)

    return new_text


def extract_html_fence(text):
    match = re.search(r"```html\s*(.*?)```", text, re.DOTALL)
    if match:
        return match.group(1).strip()
    return ""  # no fenced block found

def extract_outside_html_fence(text):
    match = re.search(r"```html\s*(.*?)```", text, re.DOTALL)
    if not match:
        return text, "" 
    
    start, end = match.span()
    
    outside_before = text[:start].strip()
    # outside_after = text[end:].strip()
    
    return outside_before