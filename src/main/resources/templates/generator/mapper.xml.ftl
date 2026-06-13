<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${mapperPackage}.${entity}Mapper">

    <resultMap id="BaseResultMap" type="${entityPackage}.${entity}">
<#list table.fields as field>
    <#if field.keyFlag>
        <id property="${field.propertyName}" column="${field.name}" />
    <#else>
        <result property="${field.propertyName}" column="${field.name}" />
    </#if>
</#list>
    </resultMap>

    <sql id="Base_Column_List">
<#list table.fields as field>
        ${field.name}<#if field_has_next>,</#if>
</#list>
    </sql>
</mapper>
