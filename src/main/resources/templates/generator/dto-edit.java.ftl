package ${dtoPackage};

import lombok.Data;

import java.io.Serializable;
<#assign hasDate = false>
<#list requestFields as field>
<#if field.propertyType == "Date">
<#assign hasDate = true>
</#if>
</#list>
<#if hasDate>
import java.util.Date;
</#if>

/**
 * ${table.comment!entity}编辑请求
 */
@Data
public class ${entity}EditRequest implements Serializable {

    /**
     * 主键 ID
     */
    private Long id;

<#list requestFields as field>
    /**
     * ${field.comment}
     */
    private ${field.propertyType} ${field.propertyName};

</#list>
    private static final long serialVersionUID = 1L;
}
