package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.InputStream
import java.nio.file.Path

/**
 * DataFrame importer의 기본 인터페이스
 */
interface DataFrameImporter {
    
    /**
     * 파일에서 DataFrame을 가져옵니다.
     * 
     * @param filePath 파일 경로
     * @return 로드된 DataFrame
     */
    fun import(filePath: Path): DataFrame
    
    /**
     * InputStream에서 DataFrame을 가져옵니다.
     * 
     * @param inputStream 입력 스트림
     * @return 로드된 DataFrame
     */
    fun import(inputStream: InputStream): DataFrame
    
    /**
     * 문자열에서 DataFrame을 가져옵니다.
     * 
     * @param content 문자열 내용
     * @return 로드된 DataFrame
     */
    fun importFromString(content: String): DataFrame
} 