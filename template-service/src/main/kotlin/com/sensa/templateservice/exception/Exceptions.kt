package com.sensa.templateservice.exception

class TemplateAlreadyExistsException(templateName: String) :
    RuntimeException("Template with name '$templateName' already exists")

class TemplateNotFoundException(templateName: String) :
    RuntimeException("Template with name '$templateName' not found")
