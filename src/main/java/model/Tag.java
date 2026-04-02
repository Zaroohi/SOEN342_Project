package model;

import java.util.Objects;

public class Tag {

    private String keyword;

//---------------------------------CONSTRUCTORS---------------------------------

    public Tag(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag keyword cannot be empty.");
        }
        this.keyword = keyword.trim();
    }

//---------------------------------GETTERS AND SETTERS---------------------------------

    public String getKeyword() {
        return this.keyword;
    }

    public void setKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag keyword cannot be empty.");
        }
        this.keyword = keyword.trim();
    }

//---------------------------------UTILITY METHODS---------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Tag tag = (Tag) o;
        return this.keyword.equalsIgnoreCase(tag.keyword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.keyword.toLowerCase());
    }

    @Override
    public String toString() {
        return this.keyword;
    }
}
