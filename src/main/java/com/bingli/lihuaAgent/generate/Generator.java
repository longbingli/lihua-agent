package com.bingli.lihuaAgent.generate;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代码生成器
 */
public class Generator {

    private static final String BASE_PACKAGE = "com.bingli.lihuaAgent";
    private static final String AUTHOR = "bingli";
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/lihua-template";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "1314";
    private static final List<String> TABLES = Collections.singletonList("user");
    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    private static final Path GENERATE_ROOT = PROJECT_ROOT.resolve("Generate");

    public static void main(String[] args) {
        List<String> tables = resolveTables(args);
        FastAutoGenerator.create(JDBC_URL, USERNAME, PASSWORD)
                .globalConfig(builder -> builder
                        .author(AUTHOR)
                        .disableOpenDir()
                        .outputDir(GENERATE_ROOT.toString())
                        .commentDate("yyyy-MM-dd HH:mm:ss"))
                .packageConfig(builder -> builder
                        .parent(BASE_PACKAGE)
                        .entity("model.entity")
                        .service("service")
                        .serviceImpl("service.impl")
                        .mapper("mapper")
                        .controller("controller")
                        .pathInfo(buildPathInfo()))
                .strategyConfig(builder -> {
                    builder.addInclude(tables);
                    builder.entityBuilder()
                            .enableFileOverride()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .idType(IdType.AUTO)
                            .logicDeleteColumnName("is_delete")
                            .logicDeletePropertyName("isDelete")
                            .addTableFills(
                                    new Column("create_time"),
                                    new Column("update_time")
                            );
                    builder.controllerBuilder()
                            .enableFileOverride()
                            .enableRestStyle()
                            .template("/templates/generator/controller.java");
                    builder.serviceBuilder()
                            .enableFileOverride()
                            .serviceTemplate("/templates/generator/service.java")
                            .serviceImplTemplate("/templates/generator/serviceImpl.java")
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl");
                    builder.mapperBuilder()
                            .enableFileOverride()
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            .disableMapperXml()
                            .formatMapperFileName("%sMapper")
                            .formatXmlFileName("%sMapper");
                })
                .injectionConfig(builder -> builder
                        .beforeOutputFile((tableInfo, objectMap) -> {
                            objectMap.put("basePackage", BASE_PACKAGE);
                            objectMap.put("entityPackage", BASE_PACKAGE + ".model.entity");
                            objectMap.put("mapperPackage", BASE_PACKAGE + ".mapper");
                            objectMap.put("servicePackage", BASE_PACKAGE + ".service");
                            objectMap.put("serviceImplPackage", BASE_PACKAGE + ".service.impl");
                            objectMap.put("controllerPackage", BASE_PACKAGE + ".controller");
                            objectMap.put("dtoPackage", BASE_PACKAGE + ".model.dto");
                            objectMap.put("voPackage", BASE_PACKAGE + ".model.vo");
                            objectMap.put("convertPackage", BASE_PACKAGE + ".convert");
                            objectMap.put("entityLower", lowerFirst(tableInfo.getEntityName()));
                            objectMap.put("queryTextFields", buildQueryTextFields(tableInfo.getFields()));
                            objectMap.put("queryExactFields", buildQueryExactFields(tableInfo.getFields()));
                            objectMap.put("requestFields", buildRequestFields(tableInfo.getFields()));
                            objectMap.put("queryFields", buildQueryFields(tableInfo.getFields()));
                            objectMap.put("voFields", buildVoFields(tableInfo.getFields()));
                        })
                        .customFile(customFiles()))
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    private static List<String> resolveTables(String[] args) {
        if (args == null || args.length == 0) {
            return TABLES;
        }
        return Arrays.stream(args)
                .flatMap(arg -> Arrays.stream(arg.split(",")))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static List<CustomFile> customFiles() {
        return List.of(
                buildJavaCustomFile("dto-add.java.ftl", "model/dto", "AddRequest.java"),
                buildJavaCustomFile("dto-edit.java.ftl", "model/dto", "EditRequest.java"),
                buildJavaCustomFile("dto-update.java.ftl", "model/dto", "UpdateRequest.java"),
                buildJavaCustomFile("dto-query.java.ftl", "model/dto", "QueryRequest.java"),
                buildJavaCustomFile("vo.java.ftl", "model/vo", "VO.java"),
                buildJavaCustomFile("convert.java.ftl", "convert", "Convert.java"),
                new CustomFile.Builder()
                        .formatNameFunction(tableInfo -> tableInfo.getEntityName() + "Mapper")
                        .fileName(".xml")
                        .templatePath("/templates/generator/mapper.xml.ftl")
                        .filePath(GENERATE_ROOT.resolve("mapper/xml").toString())
                        .enableFileOverride()
                        .build()
        );
    }

    private static CustomFile buildJavaCustomFile(String templateName, String relativeDir, String fileNameSuffix) {
        return new CustomFile.Builder()
                .formatNameFunction(tableInfo -> tableInfo.getEntityName())
                .fileName(fileNameSuffix)
                .templatePath("/templates/generator/" + templateName)
                .filePath(GENERATE_ROOT.resolve(relativeDir).toString())
                .enableFileOverride()
                .build();
    }

    private static Map<OutputFile, String> buildPathInfo() {
        Map<OutputFile, String> pathInfo = new LinkedHashMap<>();
        pathInfo.put(OutputFile.entity, GENERATE_ROOT.resolve("model/entity").toString());
        pathInfo.put(OutputFile.mapper, GENERATE_ROOT.resolve("mapper").toString());
        pathInfo.put(OutputFile.xml, GENERATE_ROOT.resolve("mapper/xml").toString());
        pathInfo.put(OutputFile.service, GENERATE_ROOT.resolve("service").toString());
        pathInfo.put(OutputFile.serviceImpl, GENERATE_ROOT.resolve("service/impl").toString());
        pathInfo.put(OutputFile.controller, GENERATE_ROOT.resolve("controller").toString());
        return pathInfo;
    }

    private static List<Map<String, Object>> buildRequestFields(List<TableField> fields) {
        return fields.stream()
                .filter(field -> !isIgnoredForRequest(field.getPropertyName()))
                .map(Generator::toFieldMap)
                .collect(Collectors.toList());
    }

    private static List<Map<String, Object>> buildQueryFields(List<TableField> fields) {
        return fields.stream()
                .filter(field -> !isIgnoredForQuery(field.getPropertyName()))
                .map(Generator::toFieldMap)
                .collect(Collectors.toList());
    }

    private static List<Map<String, Object>> buildVoFields(List<TableField> fields) {
        return fields.stream()
                .filter(field -> !isIgnoredForVo(field.getPropertyName()))
                .map(Generator::toFieldMap)
                .collect(Collectors.toList());
    }

    private static List<Map<String, Object>> buildQueryTextFields(List<TableField> fields) {
        return fields.stream()
                .filter(field -> !isIgnoredForQuery(field.getPropertyName()))
                .filter(field -> "String".equals(field.getPropertyType()))
                .filter(field -> !"sortField".equals(field.getPropertyName()) && !"sortOrder".equals(field.getPropertyName()))
                .map(Generator::toFieldMap)
                .collect(Collectors.toList());
    }

    private static List<Map<String, Object>> buildQueryExactFields(List<TableField> fields) {
        return fields.stream()
                .filter(field -> !isIgnoredForQuery(field.getPropertyName()))
                .filter(field -> !"String".equals(field.getPropertyType()))
                .map(Generator::toFieldMap)
                .collect(Collectors.toList());
    }

    private static boolean isIgnoredForRequest(String propertyName) {
        return isAuditField(propertyName) || "id".equals(propertyName);
    }

    private static boolean isIgnoredForQuery(String propertyName) {
        return "isDelete".equals(propertyName) || "userPassword".equals(propertyName);
    }

    private static boolean isIgnoredForVo(String propertyName) {
        String lowerCase = propertyName.toLowerCase(Locale.ROOT);
        return "isDelete".equals(propertyName) || lowerCase.contains("password");
    }

    private static boolean isAuditField(String propertyName) {
        return "createTime".equals(propertyName)
                || "updateTime".equals(propertyName)
                || "isDelete".equals(propertyName);
    }

    private static Map<String, Object> toFieldMap(TableField field) {
        String propertyName = field.getPropertyName();
        String comment = field.getComment();
        String propertyType = field.getPropertyType();
        return Map.of(
                "propertyName", propertyName,
                "comment", comment == null || comment.isBlank() ? propertyName : comment,
                "propertyType", propertyType
        );
    }

    private static String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
