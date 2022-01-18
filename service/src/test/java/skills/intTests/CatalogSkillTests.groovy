/**
 * Copyright 2021 SkillTree
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
package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.services.UserEventService
import skills.storage.model.DayCountItem
import skills.storage.model.SkillDef

import java.time.LocalDate

import static skills.intTests.utils.SkillsFactory.*

class CatalogSkillTests extends DefaultIntSpec {

    @Autowired
    UserEventService userEventService

    def "add skill to catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def subj1 = createSubject(1, 1)
        /* int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10, type="Skill" */
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill)

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1)

        then:
        res
        res.totalCount == 1
        res.data[0].skillId == skill.skillId
        res.data[0].projectId == skill.projectId
        res.data[0].projectName == project1.name
        res.data[0].description == skill.description
        res.data[0].pointIncrement == skill.pointIncrement
        res.data[0].numPerformToCompletion == skill.numPerformToCompletion
    }

    def "bulk export skills to catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def subj1 = createSubject(1, 1)
        def subj2 = createSubject(1, 2)
        /* int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10, type="Skill" */
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 2, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        when:
        skillsService.bulkExportSkillsToCatalog(project1.projectId, [skill.skillId, skill2.skillId, skill3.skillId])
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1, "name")

        then:
        res
        res.totalCount == 3
        res.data[0].skillId == skill.skillId
        res.data[1].skillId == skill3.skillId
        res.data[2].skillId == skill2.skillId
    }

    def "bulk export skills to catalog - 1 skill id does not exist"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def subj1 = createSubject(1, 1)
        def subj2 = createSubject(1, 2)
        /* int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10, type="Skill" */
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 2, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        when:
        skillsService.bulkExportSkillsToCatalog(project1.projectId, [skill.skillId, "haaaaaa", skill2.skillId, skill3.skillId])

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Skill [haaaaaa] doesn't exist")
    }

    def "bulk export skills to catalog - do not allow to export groups"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def subj1 = createSubject(1, 1)
        def subj2 = createSubject(1, 2)
        /* int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10, type="Skill" */
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 2, 1, 0, 1, 0, 250)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 4)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skillsGroup)

        when:
        skillsService.bulkExportSkillsToCatalog(project1.projectId, [skill.skillId, skillsGroup.skillId, skill2.skillId, skill3.skillId])

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Only type=[Skill] is supported but provided type=[SkillsGroup] for skillId=[skill4]")
    }

    def "update skill that has been exported to catalog"() {
        //changes should be reflected across all copies
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        when:
        def preEdit = skillsService.getCatalogSkills(project2.projectId, 10, 1)
        def skillNamePreEdit = skill.name
        def skillDescriptionPreEdit = skill.description
        def skillHelpUrlPreEdit = skill.helpUrl

        skill.name = "edited name"
        skill.numPerformToCompletion = 50
        skill.helpUrl = "http://newHelpUrl"
        skill.description = "updated description"
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()

        skillsService.updateSkill(skill, skill.skillId)
        def postEdit = skillsService.getCatalogSkills(project3.projectId, 10, 1)

        then:
        preEdit.data[0].name == skillNamePreEdit
        preEdit.data[0].totalPoints == 250
        preEdit.data[0].numPerformToCompletion == 1
        preEdit.data[0].description == skillDescriptionPreEdit
        preEdit.data[0].helpUrl == skillHelpUrlPreEdit
        !preEdit.data[0].selfReportingType
        postEdit.data[0].name == skill.name
        postEdit.data[0].totalPoints == 12500
        postEdit.data[0].numPerformToCompletion == 50
        postEdit.data[0].description == skill.description
        postEdit.data[0].helpUrl == skill.helpUrl
        postEdit.data[0].selfReportingType == SkillDef.SelfReportingType.Approval.toString()
    }

    def "update skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def res = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def importedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])
        importedSkill.name = "a new name"
        importedSkill.subjectId = p2subj1.subjectId
        skillsService.updateSkill(importedSkill, importedSkill.skillId)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("errorCode:ReadOnlySkill")
    }

    def "description, helpUrl, selfReportingType fields present on skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skill.name = "foo name"
        skill.numPerformToCompletion = 50
        skill.helpUrl = "http://newHelpUrl"
        skill.description = "updated description"
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def res = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def importedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])

        then:
        importedSkill.name == skill.name
        importedSkill.numPerformToCompletion == skill.numPerformToCompletion
        importedSkill.helpUrl == skill.helpUrl
        importedSkill.description == skill.description
        importedSkill.selfReportingType == skill.selfReportingType
    }

    def "remove skill from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def p2SkillsPreDelete = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def p3SkillsPreDelete = skillsService.getSkillsForSubject(project3.projectId, p3subj1.subjectId)
        skillsService.deleteSkill(skill)
        def p2Skills = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def p3Skills = skillsService.getSkillsForSubject(project3.projectId, p3subj1.subjectId)

        then:
        p2SkillsPreDelete.find {it.skillId == "skill1"}
        p3SkillsPreDelete.find {it.skillId == "skill1"}
        !p2Skills
        !p3Skills
    }

    def "bulk import skills from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, [
                [projectId:project1.projectId, skillId: skill.skillId],
                [projectId: project1.projectId, skillId: skill2.skillId]
        ])

        def skills = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)

        then:
        skills.find { it.skillId == skill.skillId }
        skills.find { it.skillId == skill2.skillId }
        !skills.find { it.skillId == skill3.skillId }
    }

    def "import skill from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        def projectPreImport = skillsService.getProject(project2.projectId)
        def subjectPreImport = skillsService.getSubject(p2subj1)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def projectPostImport1 = skillsService.getProject(project2.projectId)
        def subjectPostImport1 = skillsService.getSubject(p2subj1)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        def projectPostImport2 = skillsService.getProject(project2.projectId)
        def subjectPostImport2 = skillsService.getSubject(p2subj1)

        then:
        projectPreImport.totalPoints == 0
        projectPostImport1.totalPoints == 250
        projectPostImport2.totalPoints == 500
        subjectPreImport.totalPoints == 0
        subjectPostImport1.totalPoints == 250
        subjectPostImport2.totalPoints == 500
    }

    def "import skill from catalog twice"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("explanation:Cannot import Skill from catalog, [skill1] already exists in Project")
    }

    def "import skill that isn't shared to catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(skill)

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        then:
        def e = thrown(Exception)
        e.message.contains("explanation:Skill [skill1] from project [TestProject1] has not been shared to the catalog and may not be imported")
    }

    def "remove imported skill, should have no impact on original skill" () {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.deleteSkill([projectId: project3.projectId, subjectId: p3subj1.subjectId, skillId: skill.skillId])

        def originalSkill = skillsService.getSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: skill.skillId])
        def p2Copy = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])

        then:
        originalSkill
        originalSkill.skillId == skill.skillId
        p2Copy
        p2Copy.skillId == skill.skillId
    }

    def "report skill event on imported skill not allowed if not self-report"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project3.projectId, skillId: skill.skillId], user)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("explanation:Skills imported from the catalog can only be reported if the original skill is configured for Self Reporting")
    }

    def "report skill event on exported skill, should be reflected in all copies"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)
        def p3Stats = skillsService.getUserStats(project3.projectId, user)

        then:
        p1Stats.numSkills == 1
        p1Stats.userTotalPoints == 250
        p2Stats.numSkills == 1
        p2Stats.userTotalPoints == 250
        p3Stats.numSkills == 1
        p3Stats.userTotalPoints == 250
    }

    def "report skill event on original exported skill when original project has insufficient points"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)

        then:
        def exc = thrown(Exception)
        exc.getMessage().contains("explanation:Insufficient project points, skill achievement is disallowed, errorCode:InsufficientProjectPoints")
        skillsService.getUserStats(project2.projectId, user).userTotalPoints == 0
    }

    def "report self-report approval request on skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        assert res.body.explanation == "Skill was submitted for approval"

        def p1StatsPre = skillsService.getUserStats(project1.projectId, user)
        def p2StatsPre = skillsService.getUserStats(project2.projectId, user)

        def p1Approvals = skillsService.getApprovals(project1.projectId, 7, 1, 'requestedOn', false)
        def p2Approvals = skillsService.getApprovals(project2.projectId, 7, 1, 'requestedOn', false)

        skillsService.approve(project1.projectId, [p1Approvals.data[0].id])
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)

        then:
        p1StatsPre.numSkills == 0
        p1StatsPre.userTotalPoints == 0
        p2StatsPre.numSkills == 0
        p2StatsPre.userTotalPoints == 0
        p1Approvals.totalCount == 1
        p1Approvals.data.find { it.userId == user && it.projectId == project1.projectId && it.skillId == skill.skillId }
        p2Approvals.totalCount == 0
        p1Stats.numSkills == 1
        p1Stats.userTotalPoints == 200
        p2Stats.numSkills == 1
        p2Stats.userTotalPoints == 200
    }

    def "skill additional info endpoint must return self-report approval status for imported skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def proj2_skill4 = createSkill(2, 1, 4)
        def proj2_skill5 = createSkill(2, 1, 5)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill4.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill5.selfReportingType = SkillDef.SelfReportingType.Approval


        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.createSkill(proj2_skill4)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        skillsService.createSkill(proj2_skill5)

        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: proj2_skill4.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill3.skillId], user)
        when:
        def subjDescRes = skillsService.getSubjectDescriptions(project2.projectId, p2subj1.subjectId, user)
        then:
        subjDescRes.size() == 5
        def self1 = subjDescRes.find { it.skillId == skill.skillId }.selfReporting
        self1.enabled
        self1.type == "Approval"
        self1.requestedOn

        def self2 = subjDescRes.find { it.skillId == skill2.skillId }.selfReporting
        self2.enabled
        self2.type == "Approval"
        !self2.requestedOn

        def self3 = subjDescRes.find { it.skillId == skill3.skillId }.selfReporting
        self3.enabled
        self3.type == "Approval"
        self3.requestedOn

        def self4 = subjDescRes.find { it.skillId == proj2_skill4.skillId }.selfReporting
        self4.enabled
        self4.type == "Approval"
        self4.requestedOn

        def self5 = subjDescRes.find { it.skillId == proj2_skill5.skillId }.selfReporting
        self5.enabled
        self5.type == "Approval"
        !self5.requestedOn
    }

    def "single skill endpoint must return self-report approval status for imported skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def proj2_skill4 = createSkill(2, 1, 4)
        def proj2_skill5 = createSkill(2, 1, 5)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill4.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill5.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.createSkill(proj2_skill4)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        skillsService.createSkill(proj2_skill5)

        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: proj2_skill4.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill3.skillId], user)
        when:
        def self1 = skillsService.getSingleSkillSummary(user, project2.projectId, skill.skillId).selfReporting
        def self2 = skillsService.getSingleSkillSummary(user, project2.projectId, skill2.skillId).selfReporting
        def self3 = skillsService.getSingleSkillSummary(user, project2.projectId, skill3.skillId).selfReporting
        def self4 = skillsService.getSingleSkillSummary(user, project2.projectId, proj2_skill4.skillId).selfReporting
        def self5 = skillsService.getSingleSkillSummary(user, project2.projectId, proj2_skill5.skillId).selfReporting
        then:
        self1.enabled
        self1.type == "Approval"
        self1.requestedOn

        self2.enabled
        self2.type == "Approval"
        !self2.requestedOn

        self3.enabled
        self3.type == "Approval"
        self3.requestedOn

        self4.enabled
        self4.type == "Approval"
        self4.requestedOn

        self5.enabled
        self5.type == "Approval"
        !self5.requestedOn
    }


    def "delete user skill event for skill in catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 20
        skill.numPerformToCompletion = 10

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:

        def user = getRandomUsers(1)[0]
        Date skillDate = new Date()
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user, skillDate)
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)
        def p3Stats = skillsService.getUserStats(project3.projectId, user)

        assert p1Stats.userTotalPoints == 20
        assert p2Stats.userTotalPoints == 20
        assert p3Stats.userTotalPoints == 20

        skillsService.deleteSkillEvent([projectId: project1.projectId, skillId: skill.skillId, userId: user, timestamp: skillDate.time])

        def postDeleteP1Stats = skillsService.getUserStats(project1.projectId, user)
        def postDeleteP2Stats = skillsService.getUserStats(project2.projectId, user)
        def postDeleteP3Stats = skillsService.getUserStats(project3.projectId, user)

        then:
        postDeleteP1Stats.userTotalPoints == 0
        postDeleteP2Stats.userTotalPoints == 0
        postDeleteP3Stats.userTotalPoints == 0
    }

    def "delete honor system user skill event for skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 20
        skill.numPerformToCompletion = 5
        skill.selfReportingType = SkillDef.SelfReportingType.HonorSystem

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def timestamp = new Date()
        def res = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user, timestamp)
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)
        def p3Stats = skillsService.getUserStats(project3.projectId, user)
        assert p1Stats.userTotalPoints == 20
        assert p2Stats.userTotalPoints == 20
        assert p3Stats.userTotalPoints == 20

        skillsService.deleteSkillEvent([projectId: project1.projectId, skillId: skill.skillId, userId: user, timestamp: timestamp.time])

        def postDeleteP1Stats = skillsService.getUserStats(project1.projectId, user)
        def postDeleteP2Stats = skillsService.getUserStats(project2.projectId, user)
        def postDeleteP3Stats = skillsService.getUserStats(project3.projectId, user)

        then:
        postDeleteP1Stats.userTotalPoints == 0
        postDeleteP2Stats.userTotalPoints == 0
        postDeleteP3Stats.userTotalPoints == 0
    }

    def "get all skills exported by project"() {
        def project1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)
        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 2, 5)
        def skill6 = createSkill(1, 2, 6)

        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill4.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill5.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill6.skillId)
        Thread.sleep(1*1000)

        when:

        def pg1s1ExportedOnDesc = skillsService.getExportedSkills(project1.projectId, 1, 1, "exportedOn", false)
        def pg6s1ExportedOnDesc = skillsService.getExportedSkills(project1.projectId, 1, 6, "exportedOn", false)
        def pg1s1ExportedOnAsc = skillsService.getExportedSkills(project1.projectId, 1, 1, "exportedOn", true)
        def pg6s1ExportedOnAsc = skillsService.getExportedSkills(project1.projectId, 1, 6, "exportedOn", true)

        def pg6s6ExportedOnAsc = skillsService.getExportedSkills(project1.projectId, 1, 6, "exportedOn", false)
        def pg6s6ExportedOnDesc = skillsService.getExportedSkills(project1.projectId, 1, 6, "exportedOn", true)

        def pg1s1SkillNameAsc = skillsService.getExportedSkills(project1.projectId, 1, 1, "skillName", true)
        def pg6s1SkillNameAsc = skillsService.getExportedSkills(project1.projectId, 1, 6, "skillName", true)
        def pg1s1SkillNameDesc = skillsService.getExportedSkills(project1.projectId, 1, 1, "skillName", false)
        def pg6s1SkillNameDesc = skillsService.getExportedSkills(project1.projectId, 1, 6, "skillName", false)

        def pg1s6SkillNameAsc = skillsService.getExportedSkills(project1.projectId, 6, 1, "skillName", true)
        def pg1s6SkillNameDesc = skillsService.getExportedSkills(project1.projectId, 6, 1, "skillName", false)

        def pg1s1SubjectNameAsc = skillsService.getExportedSkills(project1.projectId, 1, 1, "subjectName", true)
        def pg6s1SubjectNameAsc = skillsService.getExportedSkills(project1.projectId, 1, 6, "subjectName", true)
        def pg1s1SubjectNameDesc = skillsService.getExportedSkills(project1.projectId, 1, 1, "subjectName", false)
        def pg6s1SubjectNameDesc = skillsService.getExportedSkills(project1.projectId, 1, 6, "subjectName", false)

        def pg1s6SubjectNameAsc = skillsService.getExportedSkills(project1.projectId, 6, 1, "subjectName", true)
        def pg1s6SubjectNameDesc = skillsService.getExportedSkills(project1.projectId, 6, 1, "subjectName", false)

        then:
        pg1s1ExportedOnDesc.data[0].skillId == skill6.skillId
        pg1s1ExportedOnDesc.totalCount == 6
        pg1s1ExportedOnDesc.count == 1
        pg6s1ExportedOnDesc.data[0].skillId == skill.skillId
        pg6s1ExportedOnDesc.totalCount == 6
        pg6s1ExportedOnDesc.count == 1

        pg1s1ExportedOnAsc.data[0].skillId == skill.skillId
        pg1s1ExportedOnAsc.totalCount == 6
        pg1s1ExportedOnAsc.count == 1
        pg6s1ExportedOnAsc.data[0].skillId == skill6.skillId
        pg6s1ExportedOnAsc.totalCount == 6
        pg6s1ExportedOnAsc.count == 1


        pg1s1SkillNameAsc.data[0].skillName == skill.name
        pg1s1SkillNameAsc.totalCount == 6
        pg1s1SkillNameAsc.count == 1
        pg6s1SkillNameAsc.data[0].skillName == skill6.name
        pg6s1SkillNameAsc.totalCount == 6
        pg6s1SkillNameAsc.count == 1

        pg1s1SkillNameDesc.data[0].skillName == skill6.name
        pg1s1SkillNameDesc.totalCount == 6
        pg1s1SkillNameDesc.count == 1
        pg6s1SkillNameDesc.data[0].skillName == skill.name
        pg6s1SkillNameDesc.totalCount == 6
        pg6s1SkillNameDesc.count == 1

        pg1s6SkillNameAsc.data[0].skillId == skill.skillId
        pg1s6SkillNameAsc.data[5].skillId == skill6.skillId
        pg1s6SkillNameAsc.totalCount == 6
        pg1s6SkillNameAsc.count == 6

        pg1s1SubjectNameAsc.data[0].subjectName == p1subj1.name
        pg1s1SubjectNameAsc.totalCount == 6
        pg1s1SubjectNameAsc.count == 1
        pg6s1SubjectNameAsc.data[0].subjectName == p1subj2.name
        pg6s1SubjectNameAsc.totalCount == 6
        pg6s1SubjectNameAsc.count == 1

        pg1s1SubjectNameDesc.data[0].subjectName == p1subj2.name
        pg1s1SubjectNameDesc.totalCount == 6
        pg1s1SubjectNameDesc.count == 1
        pg6s1SubjectNameDesc.data[0].subjectName == p1subj1.name
        pg6s1SubjectNameDesc.totalCount == 6
        pg6s1SubjectNameDesc.count == 1

        pg1s6SubjectNameAsc.data[0].subjectName == p1subj1.name
        pg1s6SubjectNameAsc.data[5].subjectName == p1subj2.name
        pg1s6SubjectNameAsc.totalCount == 6
        pg1s6SubjectNameAsc.count == 6

        pg1s6SubjectNameDesc.data[0].subjectName == p1subj2.name
        pg1s6SubjectNameDesc.data[5].subjectName == p1subj1.name
        pg1s6SubjectNameDesc.totalCount == 6
        pg1s6SubjectNameDesc.count == 6
    }

    def "get all skills imported to project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(2, 2, 4)
        def skill5 = createSkill(2, 2, 5)
        def skill6 = createSkill(2, 2, 6)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)

        def skills = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def skillsForProject = skillsService.getSkillsForProject(project2.projectId)
        def skillsForProjectWithoutImported = skillsService.getSkillsForProject(project2.projectId, "", true)

        then:
        skills.findAll { it.readOnly == true && it.copiedFromProjectId == project1.projectId && it.copiedFromProjectName == project1.name }.size() == 3
        //copiedFromProjectId, copiedFromProjectName, and readOnly are not populated by this endpoint
        skillsForProject.findAll { it.readOnly && it.copiedFromProjectId && it.copiedFromProjectName }.size() == 0

        skills.collect { it.skillId } == ["skill4subj2", "skill5subj2", "skill6subj2", "skill1", "skill2", "skill3"]
        skillsForProjectWithoutImported.collect { it.skillId } == ["skill4subj2", "skill5subj2", "skill6subj2"]
    }

    def "get exported to catalog stats for project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)

        when:

        def zeroStats = skillsService.getExportedSkillsForProjectStats(project1.projectId)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        def statsOneSkill = skillsService.getExportedSkillsForProjectStats(project1.projectId)

        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        def stats = skillsService.getExportedSkillsForProjectStats(project1.projectId)

        then:
        zeroStats.numberOfProjectsUsing == 0
        zeroStats.numberOfSkillsExported == 0

        statsOneSkill.numberOfProjectsUsing == 0
        statsOneSkill.numberOfSkillsExported == 1

        stats.numberOfProjectsUsing == 1
        stats.numberOfSkillsExported == 3
    }

    def "get imported from catalog stats for project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)
        def p2subj2 = createSubject(2, 3)
        def p4subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        def skill7 = createSkill(3, 1, 7)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p4subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(skill7)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)
        def noImports = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def oneImport = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj2.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        def threeImportsDifferentSubjects = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj2.subjectId, project3.projectId, skill7.skillId)
        def fourImporstTwoProjectsTwoSubjects = skillsService.getImportedSkillsStats(project2.projectId)

        then:
        noImports.numberOfProjectsImportedFrom == 0
        noImports.numberOfSkillsImported == 0
        oneImport.numberOfProjectsImportedFrom == 1
        oneImport.numberOfSkillsImported == 1
        threeImportsDifferentSubjects.numberOfProjectsImportedFrom == 1
        threeImportsDifferentSubjects.numberOfSkillsImported == 3
        fourImporstTwoProjectsTwoSubjects.numberOfProjectsImportedFrom == 2
        fourImporstTwoProjectsTwoSubjects.numberOfSkillsImported == 4
    }

    def "get exported skill usage stats"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)
        def p2subj2 = createSubject(2, 3)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        def skill7 = createSkill(3, 1, 7)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(skill7)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)

        def noImports = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def oneImport = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)
        def twoImports = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)

        then:
        noImports.projectId == project1.projectId
        noImports.skillId == skill.skillId
        !noImports.users

        oneImport.projectId == project1.projectId
        oneImport.skillId == skill.skillId
        oneImport.users.size() == 1
        oneImport.users.find { it.importingProjectId == project2.projectId && it.importedIntoSubjectId == p2subj1.subjectId }

        twoImports.projectId == project1.projectId
        twoImports.skillId == skill.skillId
        twoImports.users.size() == 2
        twoImports.users.find { it.importingProjectId == project2.projectId && it.importedIntoSubjectId == p2subj1.subjectId }
        twoImports.users.find { it.importingProjectId == project3.projectId && it.importedIntoSubjectId == p3subj1.subjectId }
    }

    def "get all catalog skills available to project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        p1subj1.name = "p1 test subject #1"
        def p1subj2 = createSubject(1, 2)
        def p2subj1 = createSubject(2, 2)
        def p2subj2 = createSubject(2, 3)
        def p4subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 2, 0, 20)
        def skill3 = createSkill(1, 2, 3, 0, 3, 0, 30)
        def skill4 = createSkill(1, 2, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        def skill7 = createSkill(3, 1, 7)

        def skill9 = createSkill(2, 2, 9)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p4subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(skill7)
        skillsService.createSkill(skill9)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project1.projectId, skill4.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project1.projectId, skill5.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project2.projectId, skill9.skillId)

        //numPerformToCompletion is a synthetic field, it can't be sorted on
        def availableCatalogSkills = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name")
        def availableCatalogP1 = skillsService.getCatalogSkills(project2.projectId, 2, 1, "exportedOn")
        def availableCatalogP2 = skillsService.getCatalogSkills(project2.projectId, 2, 2, "exportedOn")
        def availableCatalogP3 = skillsService.getCatalogSkills(project2.projectId, 2, 3, "exportedOn")

        def sortedByPointIncrementAsc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "pointIncrement", true)
        def sortedByPointIncrementDesc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "pointIncrement", false)
        def sortedBySubjectNameAsc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "subjectName", true)
        def sortedBySubjectNameDesc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "subjectName", false)
        def sortedByProjectNameAsc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "projectName", true)
        def sortedByProjectNameDesc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "projectName", false)

        // import skill from project1, catalog should have 5 total skills remaining available after this
        //4: project1, 1: project3
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        def postImportAvailableCatalogSkills = skillsService.getCatalogSkills(project2.projectId, 10, 1, "exportedOn")

        //search on project1.name, should result in 4 skills
        def searchOnProjectName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, project1.name)

        def searchWithPagingP1 = skillsService.getCatalogSkills(project2.projectId, 3, 1, "exportedOn", true, project1.name)
        def searchWithPagingP2 = skillsService.getCatalogSkills(project2.projectId, 3, 2, "exportedOn", true, project1.name)

        def searchOnProjectNameAndSubjectName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, project1.name, p1subj2.name)
        def searchOnProjectNameAndSubjectNameAndSkillName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, project1.name, p1subj2.name, skill3.name)

        def searchOnSubjectName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, "", p1subj1.name)

        def searchOnSkillName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, "", "", skill5.name)
        def searchOnSkillNameBroad = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, "", "", "1")

        then:
        //project2 skill should be excluded as it is the project id specified in the request
        availableCatalogSkills.totalCount == 6
        availableCatalogSkills.data.size() == 6
        //description, helpUrl, projectName
        availableCatalogSkills.data.find { it.projectId == project1.projectId && it.skillId == skill.skillId && it.subjectName == p1subj1.name && it.description && it.helpUrl && it.projectName == project1.name}
        availableCatalogSkills.count == 6
        availableCatalogSkills.data.size() == 6
        availableCatalogSkills.data[0].name == skill.name
        availableCatalogSkills.data[5].name == skill7.name

        availableCatalogP1.totalCount == 6
        availableCatalogP1.count == 2
        availableCatalogP1.data.size() == 2
        availableCatalogP1.data[0].skillId == skill.skillId
        availableCatalogP1.data[0].projectId == project1.projectId
        availableCatalogP1.data[1].skillId == skill2.skillId
        availableCatalogP1.data[1].projectId == project1.projectId

        availableCatalogP2.totalCount == 6
        availableCatalogP2.count == 2
        availableCatalogP2.data.size() == 2
        availableCatalogP2.data[0].skillId == skill3.skillId
        availableCatalogP2.data[0].projectId == project1.projectId
        availableCatalogP2.data[1].skillId == skill4.skillId
        availableCatalogP2.data[1].projectId == project1.projectId

        availableCatalogP3.totalCount == 6
        availableCatalogP3.count == 2
        availableCatalogP3.data.size() == 2
        availableCatalogP3.data[0].skillId == skill5.skillId
        availableCatalogP3.data[0].projectId == project1.projectId
        availableCatalogP3.data[1].skillId == skill7.skillId
        availableCatalogP3.data[1].projectId == project3.projectId

        sortedByPointIncrementAsc.data[5].skillId == skill3.skillId
        sortedByPointIncrementAsc.data[5].pointIncrement == 30
        sortedByPointIncrementDesc.data[0].skillId == skill3.skillId
        sortedByPointIncrementDesc.data[0].pointIncrement == 30
        sortedByProjectNameAsc.data[0].projectName == project1.name
        sortedByProjectNameAsc.data[5].projectName == project3.name
        sortedByProjectNameDesc.data[0].projectName == project3.name
        sortedByProjectNameDesc.data[5].projectName == project1.name
        postImportAvailableCatalogSkills.totalCount == 5
        postImportAvailableCatalogSkills.count == 5
        !postImportAvailableCatalogSkills.data.find { it.projectId == project1.projectId && it.skillId == skill.skillId && it.subjectName == p1subj1.name && it.description && it.helpUrl && it.projectName == project1.name}
        searchOnProjectName.count == 4
        searchOnProjectName.totalCount == 4
        searchOnProjectName.data.findAll { it.projectId == project1.projectId }.size() == 4
        searchWithPagingP1.count == 3
        searchWithPagingP1.totalCount == 4
        searchWithPagingP1.data.size() == 3 //2,3,4,5
        searchWithPagingP1.data[0].skillId == skill2.skillId
        searchWithPagingP1.data[0].projectId == project1.projectId
        searchWithPagingP1.data[1].skillId == skill3.skillId
        searchWithPagingP1.data[1].projectId == project1.projectId
        searchWithPagingP1.data[2].skillId == skill4.skillId
        searchWithPagingP1.data[2].projectId == project1.projectId
        searchWithPagingP2.count == 1
        searchWithPagingP2.totalCount == 4
        searchWithPagingP2.data.size() == 1
        searchWithPagingP2.data[0].skillId == skill5.skillId
        searchWithPagingP2.data[0].projectId == project1.projectId
        searchOnProjectNameAndSubjectName.count == 2
        searchOnProjectNameAndSubjectName.totalCount == 2
        searchOnProjectNameAndSubjectName.data.size() == 2
        searchOnProjectNameAndSubjectName.data.findAll { it.projectName == project1.name && it.subjectName == p1subj2.name }.size() == 2
        searchOnProjectNameAndSubjectNameAndSkillName.count == 1
        searchOnProjectNameAndSubjectNameAndSkillName.totalCount == 1
        searchOnProjectNameAndSubjectNameAndSkillName.data.size() == 1
        searchOnProjectNameAndSubjectNameAndSkillName.data[0].skillId == skill3.skillId
        searchOnSubjectName.count == 2
        searchOnSubjectName.totalCount == 2
        searchOnSubjectName.data.size() == 2
        searchOnSubjectName.data[0].skillId == skill2.skillId
        searchOnSubjectName.data[1].skillId == skill5.skillId
        searchOnSkillName.count == 1
        searchOnSkillName.totalCount == 1
        searchOnSkillName.data.size() == 1
        searchOnSkillName.data[0].skillId == skill5.skillId
    }

    def "get skill details should populate imported from catalog attributes"() {
        //readOnly, copiedFromProjectId, copiedFromProjectName, copiedFromSubjectName??
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def importedSkillDetails = skillsService.getSkill(["projectId": project2.projectId, "subjectId": p2subj1.subjectId, "skillId": skill.skillId])

        then:
        importedSkillDetails.readOnly == true
        importedSkillDetails.copiedFromProjectId == project1.projectId
        importedSkillDetails.copiedFromProjectName == project1.name
    }

    def "skills exported to the catalog should have sharedToCatalog populated"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        when:
        def skillDetails = skillsService.getSkill(skill)
        def subjectSkills = skillsService.getSkillsForSubject(project1.projectId, p1subj1.subjectId)
        def projectSkills = skillsService.getSkillsForProject(project1.projectId)

        then:
        skillDetails.sharedToCatalog == true
        subjectSkills.findAll { it.sharedToCatalog == true }.size() == 2
        // not populated by all project skills endpoint
        projectSkills.findAll { it.sharedToCatalog == true }.size() == 0
    }

    def "cannot export a skill with dependencies"() {
        // create skills, add dependencies, try to export top of chain
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        skillsService.assignDependency([projectId: project1.projectId, skillId: skill2.skillId, dependentSkillId: skill.skillId])

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("Skill [skill2] has dependencies. Skills with dependencies may not be exported to the catalog, errorCode:ExportToCatalogNotAllowed")
    }

    def "export skill that other skills depend on"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        skillsService.assignDependency([projectId: project1.projectId, skillId: skill2.skillId, dependentSkillId: skill.skillId])

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        def exportedSkills = skillsService.getExportedSkills(project1.projectId, 10, 1, "exportedOn", true)

        then:
        exportedSkills.totalCount == 1
        exportedSkills.count == 1
        exportedSkills.data.size() == 1
        exportedSkills.data[0].skillId == skill.skillId
    }

    def "delete a skill exported to the catalog that has been imported and added as a dependency to other skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)
        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 10)

        def p2skill1 = createSkill(2, 1, 11)
        def p2skill2 = createSkill(2, 1, 12)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p2skill2)

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.assignDependency([projectId: project1.projectId, skillId: skill2.skillId, dependentSkillId: skill.skillId])

        skillsService.deleteSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: skill.skillId])

        def importedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])
        then:
        def e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.NOT_FOUND
    }

    def "add a dependency to a skill that has been exported to the catalog"() {
        def project1 = createProject(1)

        def p1subj1 = createSubject(1, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)
        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 10)


        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:

        skillsService.exportSkillToCatalog(skill.projectId, skill4.skillId)
        skillsService.assignDependency([projectId: skill4.projectId, dependentSkillId: skill4.skillId, dependencySkillId: skill.skillId, throwExceptionOnFailure: true])

        then:
        def e = thrown(SkillsClientException)
        e.getMessage().contains("Dependencies cannot be added to a skill shared to the catalog.")
    }

    def "cannot import skill from catalog with same name as skill already existing in destination project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)

        def p2skill1 = createSkill(2, 1, 1, 0, 1, 0, 100)
        p2skill1.skillId = "foo"
        p2skill1.name = skill.name

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(skill.projectId, skill.skillId)

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        then:
        def e = thrown(SkillsClientException)
        e.getMessage().contains("Cannot import Skill from catalog, [${p2skill1.name}] already exists in Project")
    }

    def "cannot export skill to catalog if there is already a skill with the same id in the catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)

        def p2skill1 = createSkill(2, 1, 1, 0, 1, 0, 100)
        p2skill1.skillId = skill.skillId

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(skill.projectId, skill.skillId)

        when:
        skillsService.exportSkillToCatalog(p2skill1.projectId, p2skill1.skillId)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("Skill id [${p2skill1.skillId}] already exists in the catalog. Duplicated skill ids are not allowed")
    }

    def "cannot export skill to catalog if there is already a skill with the same name in the catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)

        def p2skill1 = createSkill(2, 1, 1, 0, 1, 0, 100)
        p2skill1.skillId = "rando"
        p2skill1.name = skill.name

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(skill.projectId, skill.skillId)

        when:
        skillsService.exportSkillToCatalog(p2skill1.projectId, p2skill1.skillId)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("Skill name [${p2skill1.name}] already exists in the catalog. Duplicate skill names are not allowed")
    }

    def "check if skill is already exported to the catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def p2skill1 = createSkill(2, 1, 1, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(skill.projectId, skill.skillId)

        when:
        def res1 = skillsService.doesSkillExistInCatalog(skill.projectId, skill.skillId)
        def res2 = skillsService.doesSkillExistInCatalog(skill2.projectId, skill2.skillId)

        then:
        res1.skillAlreadyInCatalog
        !res1.skillIdConflictsWithExistingCatalogSkill
        !res1.skillNameConflictsWithExistingCatalogSkill

        !res2.skillAlreadyInCatalog
        !res2.skillIdConflictsWithExistingCatalogSkill
        !res2.skillNameConflictsWithExistingCatalogSkill
    }

    def "check catalog status of multiple skillids"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(2, 1, 4)
        def skill5 = createSkill(2, 1, 5)
        def skill6 = createSkill(2, 1, 6)
        def skill7 = createSkill(2, 1, 7)
        def skill8 = createSkill(2, 1, 8)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(skill7)
        skillsService.createSkill(skill8)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        when:

        def result = skillsService.doSkillsExistInCatalog(project1.projectId, [skill.skillId, skill2.skillId, skill3.skillId, skill4.skillId, skill5.skillId])

        then:
        result
        result[skill.skillId] == true
        result[skill2.skillId] == true
        result[skill3.skillId] == false
        result[skill4.skillId] == false
        result[skill5.skillId] == false
    }

    def "validate exportability of multiple skillIds"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(2, 1, 4)
        def skill5 = createSkill(2, 1, 5)
        def skill6 = createSkill(2, 1, 6)
        def skill7 = createSkill(2, 1, 7)
        def skill8 = createSkill(2, 1, 8)

        def p3skill1 = createSkill(3, 1, 1)
        p3skill1.skillId = skill.skillId
        p3skill1.name = 'p3skill1 name'
        def p3skill2 = createSkill(3, 1, 2)
        p3skill2.skillId = 'p3skill2_skillId'
        p3skill2.name = skill2.name
        def p3skill3 = createSkill(3, 1, 3)
        p3skill3.skillId = "p3skill3_skillId"
        p3skill3.name = "p3skill3 name"
        def p3skill4 = createSkill(3, 1, 4)
        p3skill4.skillId = "p3skill4_skillId"
        p3skill4.name = "p3skill4 name"
        def p3skill5 = createSkill(3, 1, 5)
        p3skill5.skillId = "p3skill5_skillId"
        p3skill5.name = "p3skill5 name"
        def p3skill6 = createSkill(3, 1, 6)
        p3skill6.skillId = "p3skill6_skillId"
        p3skill6.name = "p3skill6 name"
        def p3skill7 = createSkill(3, 1, 7)
        p3skill7.skillId = "p3skill7_skillId"
        p3skill7.name = "p3skill7 name"

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(skill7)
        skillsService.createSkill(skill8)

        skillsService.createSkill(p3skill1)
        skillsService.createSkill(p3skill2)
        skillsService.createSkill(p3skill3)
        skillsService.createSkill(p3skill4)
        skillsService.createSkill(p3skill5)
        skillsService.createSkill(p3skill6)
        skillsService.createSkill(p3skill7)

        skillsService.assignDependency([projectId: p3skill6.projectId, dependentSkillId: p3skill6.skillId, dependencySkillId: p3skill5.skillId, throwExceptionOnFailure: true])

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project2.projectId, skill5.skillId)
        skillsService.exportSkillToCatalog(project2.projectId, skill6.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, p3skill7.skillId)


        when:
        def validationResult = skillsService.areSkillIdsExportable(project3.projectId, [p3skill1.skillId, p3skill2.skillId, p3skill3.skillId, p3skill4.skillId, p3skill5.skillId, p3skill6.skillId, p3skill7.skillId])

        then:
        validationResult[p3skill1.skillId].skillId == p3skill1.skillId
        validationResult[p3skill1.skillId].skillAlreadyInCatalog == false
        validationResult[p3skill1.skillId].skillIdConflictsWithExistingCatalogSkill == true
        validationResult[p3skill1.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult[p3skill1.skillId].hasDependencies == false

        validationResult[p3skill2.skillId].skillId == p3skill2.skillId
        validationResult[p3skill2.skillId].skillAlreadyInCatalog == false
        validationResult[p3skill2.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult[p3skill2.skillId].skillNameConflictsWithExistingCatalogSkill == true
        validationResult[p3skill2.skillId].hasDependencies == false

        validationResult[p3skill3.skillId].skillId == p3skill3.skillId
        validationResult[p3skill3.skillId].skillAlreadyInCatalog == false
        validationResult[p3skill3.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult[p3skill3.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult[p3skill3.skillId].hasDependencies == false

        validationResult[p3skill4.skillId].skillId == p3skill4.skillId
        validationResult[p3skill4.skillId].skillAlreadyInCatalog == false
        validationResult[p3skill4.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult[p3skill4.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult[p3skill4.skillId].hasDependencies == false

        validationResult[p3skill5.skillId].skillId == p3skill5.skillId
        validationResult[p3skill5.skillId].skillAlreadyInCatalog == false
        validationResult[p3skill5.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult[p3skill5.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult[p3skill5.skillId].hasDependencies == false

        validationResult[p3skill6.skillId].skillId == p3skill6.skillId
        validationResult[p3skill6.skillId].skillAlreadyInCatalog == false
        validationResult[p3skill6.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult[p3skill6.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult[p3skill6.skillId].hasDependencies == true

        validationResult[p3skill7.skillId].skillId == p3skill7.skillId
        validationResult[p3skill7.skillId].skillAlreadyInCatalog == true
        validationResult[p3skill7.skillId].skillIdConflictsWithExistingCatalogSkill == true
        validationResult[p3skill7.skillId].skillNameConflictsWithExistingCatalogSkill == true
        validationResult[p3skill7.skillId].hasDependencies == false
    }

    def "skill events are replicated across catalog skill copies"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 10, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 2, 0, 10)
        skill3.selfReportingType = SkillDef.SelfReportingType.HonorSystem.toString()

        def skill4 = createSkill(1, 1, 4, 0, 3, 0, 10)
        skill4.selfReportingType = SkillDef.SelfReportingType.Approval.toString()

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill4.skillId)

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill2.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill4.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill4.skillId)

        def user = getRandomUsers(1)[0]

        skillsService.addSkill([projectId: project1.projectId, skillId: skill4.skillId], user)
        def approvals = skillsService.getApprovals(project1.projectId, 10, 1, 'requestedOn', false)
        assert approvals.count == 1
        skillsService.approve(project1.projectId, [approvals.data[0].id])

        skillsService.addSkill([projectId: project2.projectId, skillId: skill4.skillId], user)
        def p2Approvals = skillsService.getApprovals(project2.projectId, 10, 1, 'requestedOn', false)
        assert p2Approvals.count == 0
        approvals = skillsService.getApprovals(project1.projectId, 10, 1, 'requestedOn', false)
        assert approvals.count == 1
        skillsService.approve(project1.projectId, [approvals.data[0].id])

        skillsService.addSkill([projectId: project3.projectId, skillId: skill4.skillId], user)
        def p3Approvals = skillsService.getApprovals(project2.projectId, 10, 1, 'requestedOn', false)
        assert p3Approvals.count == 0
        approvals = skillsService.getApprovals(project1.projectId, 10, 1, 'requestedOn', false)
        assert approvals.count == 1
        skillsService.approve(project1.projectId, [approvals.data[0].id])

        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        skillsService.addSkill([projectId: project1.projectId, skillId: skill2.skillId], user)
        skillsService.addSkill([projectId: project3.projectId, skillId: skill3.skillId], user)

        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        skillsService.addSkill([projectId: project1.projectId, skillId: skill2.skillId], user)
        skillsService.addSkill([projectId: project1.projectId, skillId: skill3.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill3.skillId], user)

        List<DayCountItem> skill1Project1Counts = userEventService.getUserEventCountsForSkillId(project1.projectId, skill.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill1Project2Counts = userEventService.getUserEventCountsForSkillId(project2.projectId, skill.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill1Project3Counts = userEventService.getUserEventCountsForSkillId(project3.projectId, skill.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill2Project2Counts = userEventService.getUserEventCountsForSkillId(project2.projectId, skill2.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill2Project1Counts = userEventService.getUserEventCountsForSkillId(project1.projectId, skill2.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill2Project3Counts = userEventService.getUserEventCountsForSkillId(project3.projectId, skill2.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill3Project3Counts = userEventService.getUserEventCountsForSkillId(project3.projectId, skill3.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill3Project1Counts = userEventService.getUserEventCountsForSkillId(project1.projectId, skill3.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill3Project2Counts = userEventService.getUserEventCountsForSkillId(project2.projectId, skill3.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill4Project1Counts = userEventService.getUserEventCountsForSkillId(project1.projectId, skill4.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill4Project2Counts = userEventService.getUserEventCountsForSkillId(project1.projectId, skill4.skillId, LocalDate.now().atStartOfDay().toDate())
        List<DayCountItem> skill4Project3Counts = userEventService.getUserEventCountsForSkillId(project1.projectId, skill4.skillId, LocalDate.now().atStartOfDay().toDate())

        then:
        skill1Project1Counts.size() == 1
        skill1Project1Counts[0].count == 2
        skill1Project2Counts.size() == 1
        skill1Project2Counts[0].count == 2
        skill1Project3Counts.size() == 1
        skill1Project3Counts[0].count == 2
        skill2Project2Counts.size() == 1
        skill2Project2Counts[0].count == 2
        skill2Project1Counts.size() == 1
        skill2Project1Counts[0].count == 2
        skill2Project3Counts.size() == 1
        skill2Project3Counts[0].count == 2
        skill3Project3Counts.size() == 1
        skill3Project3Counts[0].count == 3
        skill3Project1Counts.size() == 1
        skill3Project1Counts[0].count == 3
        skill3Project2Counts.size() == 1
        skill3Project2Counts[0].count == 3
        skill4Project1Counts.size() == 1
        skill4Project1Counts[0].count == 3
        skill4Project2Counts.size() == 1
        skill4Project2Counts[0].count == 3
        skill4Project3Counts.size() == 1
        skill4Project3Counts[0].count == 3
    }

    def "project badges can depend on imported skills" () {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 50)
        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        def p2badge1 = createBadge(2, 11)
        def p3badge1 = createBadge(3, 42)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)
        skillsService.createBadge(p2badge1)
        skillsService.createBadge(p3badge1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill4.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill3.skillId)

        skillsService.assignSkillToBadge(project2.projectId, p2badge1.badgeId, p2skill1.skillId)
        skillsService.assignSkillToBadge(project2.projectId, p2badge1.badgeId, skill.skillId)

        skillsService.assignSkillToBadge(project3.projectId, p3badge1.badgeId, p3skill1.skillId)
        skillsService.assignSkillToBadge(project3.projectId, p3badge1.badgeId, skill3.skillId)

        p2badge1.enabled = true
        skillsService.updateBadge(p2badge1, p2badge1.badgeId)

        p3badge1.enabled = true
        skillsService.updateBadge(p3badge1, p3badge1.badgeId)

        when:
        def user = getRandomUsers(1)[0]

        skillsService.addSkill([projectId: project2.projectId, skillId: p2skill1.skillId], user)
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)

        def p2bSumm = skillsService.getBadgeSummary(user, project2.projectId, p2badge1.badgeId)
        def p3bSumPre = skillsService.getBadgeSummary(user, project3.projectId, p3badge1.badgeId)

        skillsService.addSkill([projectId: project1.projectId, skillId: skill3.skillId], user)
        skillsService.addSkill([projectId: project3.projectId, skillId: p3skill1.skillId], user)
        def p3bSumPost = skillsService.getBadgeSummary(user, project3.projectId, p3badge1.badgeId)

        then:
        p2bSumm.numTotalSkills == 2
        p2bSumm.numSkillsAchieved == 2
        p2bSumm.badgeAchieved
        p3bSumPre.numTotalSkills == 2
        p3bSumPre.numSkillsAchieved == 0
        !p3bSumPre.badgeAchieved
        p3bSumPost.numTotalSkills == 2
        p3bSumPost.numSkillsAchieved == 2
        p3bSumPost.badgeAchieved
    }

    def "deleting imported catalog skill causes badge to be achieved if other dependencies are satisfied"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 50)
        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        def p2badge1 = createBadge(2, 11)
        def p3badge1 = createBadge(3, 42)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)
        skillsService.createBadge(p2badge1)
        skillsService.createBadge(p3badge1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill4.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill3.skillId)

        skillsService.assignSkillToBadge(project2.projectId, p2badge1.badgeId, p2skill1.skillId)
        skillsService.assignSkillToBadge(project2.projectId, p2badge1.badgeId, skill.skillId)

        skillsService.assignSkillToBadge(project3.projectId, p3badge1.badgeId, p3skill1.skillId)
        skillsService.assignSkillToBadge(project3.projectId, p3badge1.badgeId, skill3.skillId)

        p2badge1.enabled = true
        skillsService.updateBadge(p2badge1, p2badge1.badgeId)

        p3badge1.enabled = true
        skillsService.updateBadge(p3badge1, p3badge1.badgeId)

        when:
        def user = getRandomUsers(1)[0]

        skillsService.addSkill([projectId: project2.projectId, skillId: p2skill1.skillId], user)
        skillsService.deleteSkill(skill)

        def p2bSumm = skillsService.getBadgeSummary(user, project2.projectId, p2badge1.badgeId)
        def p3bSum = skillsService.getBadgeSummary(user, project3.projectId, p3badge1.badgeId)

        then:
        p2bSumm.numTotalSkills == 1
        p2bSumm.numSkillsAchieved == 1
        p2bSumm.badgeAchieved
        p3bSum.numTotalSkills == 2
        p3bSum.numSkillsAchieved == 0
        !p3bSum.badgeAchieved
    }

    def "imported skills as dependency should not be returned as potential dependencies for global badges"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        def p2badge1 = createBadge(2, 11)
        def p3badge1 = createBadge(3, 42)

        skill.name = "Sample Name Query Test"

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)
        skillsService.createBadge(p2badge1)
        skillsService.createBadge(p3badge1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        def supervisorService = createSupervisor()

        def badge = SkillsFactory.createBadge()
        badge.enabled = true
        supervisorService.createGlobalBadge(badge)

        when:
        def res = supervisorService.getAvailableSkillsForGlobalBadge(badge.badgeId, "Sample")

        then:
        res.totalAvailable == 1
        res.suggestedSkills.findAll {it.name == 'Sample Name Query Test'}.size() == 1
    }

    def "cannot assign skill imported from catalog as a dependency to a global badge"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        def p2badge1 = createBadge(2, 11)
        def p3badge1 = createBadge(3, 42)

        skill.name = "Sample Name Query Test"

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)
        skillsService.createBadge(p2badge1)
        skillsService.createBadge(p3badge1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        def supervisorService = createSupervisor()

        def badge = SkillsFactory.createBadge()
        badge.enabled = true
        supervisorService.createGlobalBadge(badge)

        when:
        supervisorService.assignSkillToGlobalBadge([badgeId: badge.badgeId, projectId: project3.projectId, skillId: skill.skillId])

        then:
        def e = thrown(SkillsClientException)
        e.message.contains('Imported Skills may not be added as Global Badge Dependencies')
    }

    def "cannot share via cross project skills that have been imported from the catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill2.skillId)

        when:
        skillsService.shareSkill(project2.projectId, skill.skillId, project3.projectId)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("Skills imported from the catalog may not be shared as cross project dependencies")
    }

    def "skills imported from the catalog cannot be shared as cross project dependencies to all projects"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill2.skillId)

        when:
        skillsService.shareSkill(project2.projectId, skill.skillId, "ALL_SKILLS_PROJECTS")

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("Skills imported from the catalog may not be shared as cross project dependencies")
    }

    def "skills exported to the catalog can be shared as cross project dependencies"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)


        when:
        skillsService.shareSkill(project1.projectId, skill.skillId, project2.projectId)
        def sharedSkills = skillsService.getSharedWithMeSkills(project2.projectId)

        then:
        sharedSkills.size() == 1
        sharedSkills[0].skillName == skill.name
        sharedSkills[0].projectId == project1.projectId
    }

    def "skills exported to the catalog can be shared as cross project dependencies with all projects"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)


        when:
        skillsService.shareSkill(project1.projectId, skill.skillId, "ALL_SKILLS_PROJECTS")
        def sharedSkills = skillsService.getSharedWithMeSkills(project2.projectId)

        then:
        sharedSkills.size() == 1
        sharedSkills[0].skillName == skill.name
        sharedSkills[0].projectId == project1.projectId
    }

}