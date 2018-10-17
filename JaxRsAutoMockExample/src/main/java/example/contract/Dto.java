package example.contract;


public class Dto {
    private String field;
    private Integer otherField;

    public Dto(){

    }

    public Dto(String field, Integer otherField) {
        this.field = field;
        this.otherField = otherField;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Integer getOtherField() {
        return otherField;
    }

    public void setOtherField(Integer otherField) {
        this.otherField = otherField;
    }
}

