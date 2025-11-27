prompt_image_summarisation = [
    (
        "user",
        [
            {
                "type": "text",
                "text": """Describe the image in detail.
                Be specific about graphs and tables, such as bar plots."""
            },
            {
                "type": "image_url",
                "image_url": {"url": "data:image/jpeg;base64,{image}"},
            },
        ],
    )
]