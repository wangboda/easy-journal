package bd.share.journal.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "journal"
)
public class JournalProperty {
    private Boolean enabled;
    private Boolean logAnyway;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getLogAnyway() {
        return logAnyway;
    }

    public void setLogAnyway(Boolean logAnyway) {
        this.logAnyway = logAnyway;
    }
}
