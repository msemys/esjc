package com.github.msemys.esjc.user;

/**
 * Represents hypermedia link describing action allowed on user resource.
 */
public class RelLink {

    /**
     * Location of the resource.
     */
    public final String href;

    /**
     * Relationship.
     */
    public final String rel;

    public RelLink(String href, String rel) {
        this.href = href;
        this.rel = rel;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RelLink{");
        sb.append("href='").append(href).append('\'');
        sb.append(", rel='").append(rel).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
