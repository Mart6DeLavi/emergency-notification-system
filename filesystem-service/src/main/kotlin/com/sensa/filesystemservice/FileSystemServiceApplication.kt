package com.sensa.filesystemservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@EnableDiscoveryClient
@SpringBootApplication
class FileSystemServiceApplication

fun main(args: Array<String>) {
    runApplication<FileSystemServiceApplication>(*args)
}
