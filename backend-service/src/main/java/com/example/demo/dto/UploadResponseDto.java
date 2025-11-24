package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UploadResponseDto {
    private String filename;
    
    @JsonProperty("file_id")
    private String fileId;
    
    @JsonProperty("task_id")
    private String taskId;
    
    @JsonProperty("parse_summarise_ingest_task_id")
    private String parseSummariseIngestTaskId;
    
    private String path;
    
    @JsonProperty("qdrant_collection_name")
    private String qdrantCollectionName;
    
    @JsonProperty("flash_cards")
    private FlashCardListDto flashCards;
    
    private VideoDto video;

    @JsonProperty("process_xml_task_id")
    private String processXmlTaskId;

    @JsonProperty("collection_name")
    private String collectionName;
}