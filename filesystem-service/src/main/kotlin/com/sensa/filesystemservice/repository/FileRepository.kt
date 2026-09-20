package com.sensa.filesystemservice.repository

import com.sensa.filesystemservice.entity.FileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FileRepository : JpaRepository<FileEntity, Long> {

    fun findByIdAndUserId(id: Long, userId: java.util.UUID): FileEntity?

    fun deleteByIdAndUserId(id: Long, userId: java.util.UUID): Int
}
