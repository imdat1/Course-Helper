prompt_image_summarisation = [
    (
        "user",
        [
            {
                "type": "text",
                "text": """Describe the image in detail.
                If there's a table, try to represent it accurately in markdown format.
                Be specific about graphs and tables, such as bar plots."""
            },
            {
                "type": "image_url",
                "image_url": {"url": "data:image/jpeg;base64,{image}"},
            },
        ],
    )
]