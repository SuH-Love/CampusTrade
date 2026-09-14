CREATE TABLE IF NOT EXISTS t_ai_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    file_url VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型: pdf/word/image/text',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    status VARCHAR(20) DEFAULT 'processing' COMMENT '处理状态: processing/ready/error',
    chunk_count INT DEFAULT 0 COMMENT '分块数量',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_file_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI文档知识库';

CREATE TABLE IF NOT EXISTS t_ai_document_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL COMMENT '所属文档ID',
    chunk_index INT NOT NULL COMMENT '分块索引',
    content TEXT NOT NULL COMMENT '分块文本内容',
    page_num INT COMMENT '来源页码(PDF)',
    image_urls TEXT COMMENT 'chunk内图片URL(JSON数组)',
    metadata TEXT COMMENT '元数据(JSON,章节标题等)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0,
    INDEX idx_document (document_id),
    INDEX idx_chunk (document_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI文档分块';