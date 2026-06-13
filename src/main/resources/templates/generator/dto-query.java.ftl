package ${dtoPackage};

import ${basePackage}.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
<#assign hasDate = false>
<#list queryFields as field>
<#if field.propertyType == "Date">
<#assign hasDate = true>
</#if>
</#list>
<#if hasDate>
import java.util.Date;
</#if>

/**
 * ${table.comment!entity}查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ${entity}QueryRequest extends PageRequest implements Serializable {

<#list queryFields as field>
    /**
     * ${field.comment}
     */
    private ${field.propertyType} ${field.propertyName};

</#list>
    private static final long serialVersionUID = 1L;
}
