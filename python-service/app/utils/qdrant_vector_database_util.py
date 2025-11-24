from qdrant_client import QdrantClient, models

# Retrieve random documents from Qdrant using embeddings
def get_random_data_from_qdrant(qdrant_client: QdrantClient, collection_name:str, num_docs=5):
    return_list = [] 

    response = qdrant_client.query_points(
        collection_name=collection_name,
        query=models.SampleQuery(sample=models.Sample.RANDOM),
        limit=num_docs,
        with_payload=True
    )
        
    for hit in response.points:
        if hit.payload['doc_type'] == "image":
            return_list.append(hit.payload['summary'])
        else:
            return_list.append(hit.payload['full_content'])
    
    return return_list

def get_closest_points_from_qdrant(
        client: QdrantClient,
        collection_name:str,
        embedded_query: list[float],
        top_k=5
    ):
    results = client.search(
            collection_name=collection_name,
            query_vector=embedded_query,
            limit=top_k,
            with_payload=True,
        )

    results_content = []
    for result in results:
        payload = result.payload or {}
        doc_type = payload.get("doc_type")
        if doc_type == "image":
            results_content.append(payload.get("summary"))
        else:
            results_content.append(payload.get("full_content"))
    return results_content