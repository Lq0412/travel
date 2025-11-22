package com.lq.travel.model.enums;


import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("普通用户", "user"),
    MERCHANT("商家", "merchant"),
    ADMIN("管理员", "admin");
    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }


    /**
     * 根据 text 获取枚举
     *
     * @param text 文本值
     * @return 对应的枚举值
     */

    public static UserRoleEnum getValueByText(String text) {
        for (UserRoleEnum value : UserRoleEnum.values()) {
            if (value.getText().equals(text)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 对应的枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        for (UserRoleEnum valueEnum : UserRoleEnum.values()) {
            if (valueEnum.getValue().equals(value)) {
                return valueEnum;
            }
        }
        return null;
    }
}
