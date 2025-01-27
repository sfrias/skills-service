/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.services.settings

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.storage.model.Setting

@Component
class ProjectSettingsValidator {
    @Autowired
    SettingsDataAccessor settingsDataAccessor

    public void validate(ProjectSettingsRequest settingsRequest) {
        if (settingsRequest.setting == Settings.PRODUCTION_MODE.settingName && settingsRequest.isEnabled()) {
            Setting setting = settingsDataAccessor.getProjectSetting(settingsRequest.projectId, Settings.INVITE_ONLY_PROJECT.settingName)
            if (setting?.isEnabled()) {
                throw new SkillException("${Settings.PRODUCTION_MODE.settingName} can only be enabled if ${Settings.INVITE_ONLY_PROJECT.settingName} is false")
            }
        } else if (settingsRequest.setting == Settings.INVITE_ONLY_PROJECT.settingName && settingsRequest.isEnabled()) {
            Setting setting = settingsDataAccessor.getProjectSetting(settingsRequest.projectId, Settings.PRODUCTION_MODE.settingName)
            if (setting?.isEnabled()) {
                throw new SkillException("${Settings.INVITE_ONLY_PROJECT.settingName} can only be enabled if ${Settings.PRODUCTION_MODE.settingName} is false")
            }
        }
    }

    public void validate(List<ProjectSettingsRequest> settings) {
        ProjectSettingsRequest discoverable = settings.find {it.setting == Settings.PRODUCTION_MODE.settingName }
        ProjectSettingsRequest inviteOnly = settings.find { it.setting == Settings.INVITE_ONLY_PROJECT.settingName }

        if (discoverable != null ^ inviteOnly != null) {
            validate(discoverable ?: inviteOnly)
        } else if (discoverable && inviteOnly) {
            if (discoverable?.isEnabled() && inviteOnly?.isEnabled()) {
                throw new SkillException("${discoverable.setting} and ${inviteOnly.setting} can not both be enabled")
            }
        }

    }
}
