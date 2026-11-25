package com.swp391.g1.model;

import java.util.ArrayList;
import java.util.List;

public class RegistrationFormField {
    private int id;
    private int formId; // id form
    private String fieldKey; // mã định danh hoạt động
    private String label; // nội dung câu hỏi
    private String fieldType; // loại câu hỏi
    private boolean required; // bắt buộc trả lời
    private String options; // danh sách đáp án cho multiple choice
    private boolean saveToProfile; // lưu thông tin vào hồ sơ sinh viên tái sử dụng
    private int sortOrder; // thứ tự hiển thị câu hỏi trong form
    private String value; // giá trị trả lời
    private List<String> selectedOptions = new ArrayList<>(); // danh sách lựa chọn cho câu hỏi nhiều đáp án

    public int getId() {
        return id;
    }

    public void setId(int v) {
        id = v;
    }

    public int getFormId() {
        return formId;
    }

    public void setFormId(int v) {
        formId = v;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String v) {
        fieldKey = v;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String v) {
        label = v;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String v) {
        fieldType = v;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean v) {
        required = v;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String v) {
        options = v;
    }

    public int getOptionCount() {
        return getOptionList().size();
    }

    public List<String> getOptionList() {
        return options == null || options.isBlank()
                ? List.of()
                : options.lines().map(String::trim).filter(option -> !option.isEmpty()).toList();
    }

    public boolean isSaveToProfile() {
        return saveToProfile;
    }

    public void setSaveToProfile(boolean v) {
        saveToProfile = v;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int v) {
        sortOrder = v;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String v) {
        value = v;
    }

    public List<String> getSelectedOptions() {
        return selectedOptions;
    }

    public void setSelectedOptions(List<String> selectedOptions) {
        this.selectedOptions = selectedOptions == null ? new ArrayList<>() : new ArrayList<>(selectedOptions);
    }
}
