from qdrant_client import QdrantClient
from qdrant_client.http.models import Filter, FieldCondition, MatchValue
from app.core.config_settings import get_settings

settings = get_settings()

def delete_points_for_file(collection_name: str, file_id: str) -> int:
	"""Delete points in Qdrant collection where payload file_id matches provided file_id.
	Returns the number of points requested for deletion (Qdrant returns operation ID; here we'll attempt and return 0/unknown).
	"""
	client = QdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)
	flt = Filter(must=[FieldCondition(key="file_id", match=MatchValue(value=file_id))])
	res = client.delete(collection_name=collection_name, points_selector=flt)
	# Qdrant delete returns an operation result; not count. We return 0 to indicate request made.
	return 0


def delete_collection(collection_name: str) -> bool:
	"""Delete an entire Qdrant collection by name. Returns True if request was issued."""
	client = QdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)
	# Qdrant Python client raises if collection doesn't exist; caller should treat success as idempotent
	client.delete_collection(collection_name=collection_name)
	return True
