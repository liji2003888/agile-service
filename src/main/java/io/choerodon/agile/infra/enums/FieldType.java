package io.choerodon.agile.infra.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author shinan.chen
 * @date 2019/3/29
 */
public class FieldType {
    private FieldType() {
    }
    
    public class RulePredefind{
        private RulePredefind(){
        }
        
        public static final String LONG_TYPE = "long";
        public static final String DATE_TYPE = "date";
        public static final String TEXT_TYPE = "text";
        public static final String DECIMAL_TYPE = "decimal";
    }

    public static final String TEXT = "text";
    public static final String RADIO = "radio";
    public static final String CHECKBOX = "checkbox";
    public static final String TIME = "time";
    public static final String DATETIME = "datetime";
    public static final String NUMBER = "number";
    public static final String INPUT = "input";
    public static final String SINGLE = "single";
    public static final String MULTIPLE = "multiple";
    public static final String MEMBER = "member";
    public static final String DATE = "date";
    public static final String MULTI_MEMBER = "multiMember";

    public static final List<String> multipleFieldType = Collections.unmodifiableList(Arrays.asList(CHECKBOX, MULTIPLE, MULTI_MEMBER));

    public static Boolean hasOption(String typeName) {
        return typeName.equals(RADIO) || typeName.equals(CHECKBOX) || typeName.equals(SINGLE) || typeName.equals(MULTIPLE);
    }
}
