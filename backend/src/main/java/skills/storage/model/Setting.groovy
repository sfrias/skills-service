package skills.storage.model

import groovy.transform.ToString

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = 'settings')
@ToString(includeNames = true)
class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    //nullable
    String settingGroup

    //nullable
    String projectId

    //non-null
    String setting

    //non-null
    String value

    //convenience method for settings that are in an either on or off state as opposed to containing a meaningful value
    boolean isEnabled(){
        return Boolean.valueOf(value) || value.toLowerCase() == "enabled" || value.toLowerCase() == "enable" || value.toLowerCase() == "on"
    }
}