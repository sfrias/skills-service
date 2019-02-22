package skills.service.datastore.services.settings.listeners

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.service.controller.request.model.SettingsRequest
import skills.service.controller.result.model.LevelDefinitionRes
import skills.service.datastore.services.LevelDefinitionStorageService
import skills.service.datastore.services.settings.SettingChangedListener
import skills.service.datastore.services.settings.Settings
import skills.storage.model.LevelDef
import skills.storage.model.ProjDef
import skills.storage.model.Setting
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo

import javax.transaction.Transactional

@Slf4j
@Component
class LevelPointsSettingListener implements SettingChangedListener{

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    LevelDefRepo levelDefRepo

    @Override
    boolean supports(SettingsRequest setting) {
        return setting.setting == Settings.LEVEL_AS_POINTS.settingName
    }

    @Transactional
    @Override
    void execute(Setting previousValue, SettingsRequest setting) {
        ProjDef project = projDefRepo.findByProjectId(setting.projectId)

        if(setting.isEnabled() && (!previousValue?.isEnabled())){
            log.info("converting all levels for project [${setting.projectId}] (including skill levels) to points")
            convertToPoints(project.levelDefinitions, project.totalPoints)
            project.subjects?.each{
                convertToPoints(it.levelDefinitions, it.totalPoints)
            }
        }else if(!setting.isEnabled()){
            log.info("convering all levels for project [${setting.projectId}] (including skill levels) to percentages")
            convertToPercentage(project.levelDefinitions, project.totalPoints)
            project.subjects?.each{
                convertToPercentage(it.levelDefinitions, it.totalPoints)
            }
        }
    }

    private void convertToPoints(List<LevelDef> levelDefs, int totalPoints){
        List<Integer> levelScores = levelDefs.sort({ it.level }).collect {
            return (int) (totalPoints * (it.percent / 100d))
        }
        levelDefs.eachWithIndex{ LevelDef entry, int i ->
            Integer fromPts = levelScores.get(i)
            /*if(i > 0){
                fromPts+=1
            }*/
            Integer toPts = (i != levelScores.size() - 1) ? levelScores.get(i + 1) : null
            entry.pointsFrom = fromPts
            entry.pointsTo = toPts
            levelDefRepo.save(entry)
        }
    }

    private void convertToPercentage(List<LevelDef> levelDefs, int totalPoints){
        levelDefs.eachWithIndex { LevelDef entry, int i ->
            entry.percent = (int) (((double) entry.pointsFrom / totalPoints) * 100d)
            levelDefRepo.save(entry)
        }
    }
}