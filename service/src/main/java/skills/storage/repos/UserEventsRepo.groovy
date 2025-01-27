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
package skills.storage.repos

import groovy.transform.CompileStatic
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.DayCountItem
import skills.storage.model.EventCount
import skills.storage.model.EventType
import skills.storage.model.LabeledCount
import skills.storage.model.UserEvent
import skills.storage.model.WeekCountItem

import javax.persistence.QueryHint
import java.util.stream.Stream

@CompileStatic
interface UserEventsRepo extends CrudRepository<UserEvent, Integer> {

    @Nullable
    @Query(value="""SELECT min(all_events.project_id) as projectId, all_events.week_number as weekNumber, sum(all_events.count) as count FROM
        (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue INNER JOIN (
                    select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                    from skill_definition sd 
                    where sd.type = 'Skill' and sd.id = :skillRefId and 
                    sd.enabled = 'true'
                ) def ON uue.skill_ref_id = def.id
            WHERE
                    uue.event_time >= :start 
        ) all_events
        GROUP BY all_events.week_number
        ORDER BY all_events.week_number DESC
    """, nativeQuery = true)
    Stream<WeekCountItem> getEventCountForSkillGroupedByWeek(@Param("skillRefId") Integer skillRefId, @Param("start") Date start)

    @Nullable
    @Query(value="""SELECT min(all_events.project_id) as projectId, all_events.event_time as day, sum(all_events.count) as count FROM
        (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue INNER JOIN (
                    select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                    from skill_definition sd 
                    where sd.type = 'Skill' and sd.id = :skillRefId and 
                    sd.enabled = 'true'
                ) def ON uue.skill_ref_id = def.id
            WHERE
                    uue.event_time >= :start AND 
                    uue.event_type = :#{#type.name()}
        ) all_events
        GROUP BY all_events.event_time
        ORDER BY all_events.event_time DESC
    """, nativeQuery = true)
    Stream<DayCountItem> getEventCountForSkill(@Param("skillRefId") Integer skillRefId, @Param("start") Date start, @Param("type") EventType type)

    @Nullable
    @Query(value="""SELECT min(event_join.project_id) as projectId, event_join.event_time as day, count(distinct event_join.user_id) as count FROM
        (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
            INNER JOIN (
                select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                from skill_definition sd 
                where sd.type = 'Skill' and (sd.id = :skillRefId or sd.copied_from_skill_ref = :skillRefId) and 
                sd.enabled = 'true'
            ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time >= :start AND
                uue.event_type = :#{#type.name()}
        ) event_join
        GROUP BY event_join.event_time
        ORDER BY event_join.event_time DESC
    """, nativeQuery = true)
    Stream<DayCountItem> getDistinctUserCountForSkill(@Param("skillRefId") Integer skillRefId, @Param("start") Date start, @Param("type") EventType type)

    @Nullable
    @Query(value="""SELECT min(event_join.project_id) as projectId, event_join.week_number as weekNumber, count(distinct event_join.user_id) as count FROM
        (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
            INNER JOIN (
                select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                from skill_definition sd 
                where sd.type = 'Skill' and (sd.id = :skillRefId or sd.copied_from_skill_ref = :skillRefId) and 
                sd.enabled = 'true'
            ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time >= :start
        ) event_join
        GROUP BY event_join.week_number
        ORDER BY event_join.week_number DESC
    """, nativeQuery = true)
    Stream<WeekCountItem> getDistinctUserCountForSkillGroupedByWeek(@Param("skillRefId") Integer skillRefId, @Param("start") Date start)

    @Query(value="""select min(all_events.project_id) as projectId, all_events.event_time as day, sum(all_events.count) as count from
         (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
            INNER JOIN (
                select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                from skill_definition sd 
                where sd.type = 'Skill' and 
                sd.enabled = 'true' and
                sd.id in (select rel.child_ref_id from skill_relationship_definition rel where rel.parent_ref_id = :subjectRawId)
            ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time >= :start AND
                uue.event_type = :#{#type.name()}
        ) all_events
        group by all_events.event_time 
        order by all_events.event_time desc
    """, nativeQuery=true)
    Stream<DayCountItem> getEventCountForSubject(@Param("subjectRawId") Integer subjectRawId, @Param("start") Date start, @Param("type") EventType type)

    @Nullable
    @Query(value="""select min(all_events.project_id) as projectId, all_events.week_number as weekNumber, sum(all_events.count) as count from
         (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
            INNER JOIN (
                select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                from skill_definition sd 
                where sd.type = 'Skill' and 
                sd.enabled = 'true' and
                sd.id in (select rel.child_ref_id from skill_relationship_definition rel where rel.parent_ref_id = :subjectRawId)
            ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time >= :start
        ) all_events
        group by all_events.week_number 
        order by all_events.week_number desc
    """, nativeQuery = true)
    Stream<WeekCountItem> getEventCountForSubjectGroupedByWeek(@Param("subjectRawId") Integer subjectRawId, @Param("start") Date start)

    @Query(value="""select min(all_events.project_id) as projectId, all_events.week_number as weekNumber, count(distinct all_events.user_id) as count from
         (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
            INNER JOIN (
                select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                from skill_definition sd 
                where sd.type = 'Skill' and 
                sd.enabled = 'true' and
                sd.id in (select rel.child_ref_id from skill_relationship_definition rel where rel.parent_ref_id = :skillRefId)
            ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time >= :start
        ) all_events
        group by all_events.week_number 
        order by all_events.week_number desc
    """, nativeQuery = true)
    Stream<WeekCountItem> getDistinctUserCountForSubjectGroupedByWeek(@Param("skillRefId") Integer skillRefId, @Param("start") Date start)

    @Query(value="""select min(all_events.project_id) as projectId, all_events.event_time as day, count(distinct all_events.user_id) as count from
         (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
            INNER JOIN (
                select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                from skill_definition sd 
                where sd.type = 'Skill' and 
                sd.enabled = 'true' and
                sd.id in (select rel.child_ref_id from skill_relationship_definition rel where rel.parent_ref_id = :skillRefId)
            ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time > :start AND
                uue.event_type = :#{#type.name()}
        ) all_events
        group by all_events.event_time 
        order by all_events.event_time desc
        
    """, nativeQuery = true)
    Stream<DayCountItem> getDistinctUserCountForSubject(@Param("skillRefId") Integer skillRefId, @Param("start") Date start, @Param("type") EventType type)


    @Query(value="""
        select min(all_events.project_id) as projectId, all_events.event_time as day, sum(all_events.count) as count from
         (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
            INNER JOIN (
                select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                from skill_definition sd 
                where sd.type = 'Skill' 
                and sd.project_id = :projectId
                and sd.enabled = 'true'
            ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time >= :start AND
                uue.event_type = :#{#type.name()}
        ) all_events
        group by all_events.event_time 
        order by all_events.event_time desc
    """, nativeQuery = true)
    Stream<DayCountItem> getEventCountForProject(@Param("projectId") String projectId, @Param("start") Date start, @Param("type") EventType type)

    @Query(value="""select min(all_events.project_id) as projectId, all_events.week_number as weekNumber, sum(all_events.count) as count from
         (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
            INNER JOIN (
                select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                from skill_definition sd 
                where sd.type = 'Skill' 
                and sd.project_id = :projectId
                and sd.enabled = 'true'
            ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time >= :start
        ) all_events
        group by all_events.week_number 
        order by all_events.week_number desc
    """, nativeQuery = true)
    Stream<WeekCountItem> getEventCountForProjectGroupedByWeek(@Param("projectId") String projectId, @Param("start") Date start)

    @Query(value="""
        SELECT min(uee.project_id) as projectId, uee.event_time as day, sum(uee.count) as count FROM
        (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
            INNER JOIN (
                select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                from skill_definition sd 
                where sd.type = 'Skill' 
                and sd.project_id in :projectIds
                and sd.enabled = 'true'
            ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time > :start AND
                uue.event_type = :#{#type.name()} AND
                uue.user_id = :userId
        ) uee
        GROUP BY uee.project_id, uee.event_time
        ORDER BY uee.event_time DESC
    """, nativeQuery = true)
    Stream<DayCountItem> getEventCountForUser(@Param("userId") String userId, @Param("start") Date start, @Param("type") EventType type, @Param("projectIds") List<String> projectIds)

    @Query(value="""
        SELECT all_events.project_id as projectId, all_events.week_number as weekNumber, sum(all_events.count) as count FROM
        (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue INNER JOIN (
                    select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                    from skill_definition sd 
                    where sd.type = 'Skill' 
                    and sd.project_id in :projectIds
                    and sd.enabled = 'true'
                ) def ON uue.skill_ref_id = def.id
            WHERE
                    uue.event_time >= :start AND
                    uue.user_id = :userId
        ) all_events
        GROUP BY all_events.project_id, all_events.week_number
        ORDER BY all_events.week_number DESC
    """, nativeQuery = true)
    Stream<WeekCountItem> getEventCountForUserGroupedByWeek(@Param("userId") String userId, @Param("start") Date start, @Param("projectIds") List<String> projectIds)


    @Query(value="""
        select min(all_events.project_id) as projectId, all_events.event_time as day, count(distinct all_events.user_id) as count from 
        (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue INNER JOIN (
                    select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                    from skill_definition sd 
                    where sd.type = 'Skill' 
                    and sd.project_id = :projectId
                    and sd.enabled = 'true'
                ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time >= :start AND
                uue.event_type = :#{#type.name()}
        ) all_events
        group by all_events.event_time 
        order by all_events.event_time desc
    """, nativeQuery = true)
    Stream<DayCountItem> getDistinctUserCountForProject(@Param("projectId") String projectId, @Param("start") Date start, @Param("type") EventType type)

    @Query(value="""
        select new skills.storage.model.EventCount(ue.eventTime, count(distinct ue.userId), ue.eventType) from UserEvent ue
        where ue.eventTime > :start AND
        ue.skillRefId in (
            SELECT case when sd.copiedFrom is not null then sd.copiedFrom else sd.id end as id 
            FROM SkillDef sd 
            WHERE sd.projectId = :projectId 
            AND sd.type = 'Skill'
            and sd.enabled = 'true'
        )   
        group by ue.eventTime, ue.eventType
        order by ue.eventTime desc
    """)
    Stream<EventCount> getDistinctUserCountForProject(@Param("projectId") String projectId, @Param("start") Date start)

    @Query(value="""
        select min(ue.projectId) as projectId, ue.weekNumber as weekNumber, count(distinct ue.userId) as count from UserEvent ue
        where ue.eventTime >= :start AND
        ue.skillRefId in (
            select case when sd.copiedFrom is not null then sd.copiedFrom else sd.id end as id 
            from SkillDef sd 
            WHERE sd.projectId = :projectId 
            AND sd.type = 'Skill'
            AND sd.enabled = 'true'
        )   
        group by ue.weekNumber
        order by ue.weekNumber desc
    """)
    Stream<WeekCountItem> getDistinctUserCountForProjectGroupedByWeek(@Param("projectId") String projectId, @Param("start") Date start)

    @Nullable
    Stream<UserEvent> findAllBySkillRefIdAndEventType(Integer skillRefId, EventType type)

    @Nullable
    UserEvent findTopByProjectIdOrderByEventTimeDesc(String projectId)

    @QueryHints(value = [
        @QueryHint(name = "org.hibernate.cacheable", value = "false"),
        @QueryHint(name = "org.hibernate.readOnly", value = "true")
    ])
    @Nullable
    Stream<UserEvent> findAllByEventTypeAndEventTimeLessThan(EventType type, Date start)

    void deleteByEventTypeAndEventTimeLessThan(EventType type, Date start)

    @Modifying
    @Query(value='''
        update UserEvent ue
        set ue.count = ue.count-1 
        where ue.skillRefId = :skillRefId
        and ue.userId = :userId
        and ue.eventTime = :eventTime
        and ue.eventType = :eventType
        and ue.count > 0
    ''')
    void decrementEventCount(@Param("eventTime") Date eventDate, @Param("userId") String userId, @Param("skillRefId") Integer skillRefId, @Param("eventType") EventType type)

    @Nullable
    UserEvent findByUserIdAndSkillRefIdAndEventTimeAndEventType(String userId, Integer skillRefId, Date eventTime, EventType type)

    @Nullable
    @Query(value='''
        SELECT COUNT(ue.user_id) OVER() 
        FROM user_events ue, (
            SELECT user_id, achieved_on FROM user_achievement 
            WHERE skill_ref_id = :skillRefId
        ) AS achievements 
        WHERE 
            ue.skill_ref_id = :skillRefId 
            AND ue.user_id = achievements.user_id 
            AND ue.event_time > achievements.achieved_on 
        GROUP BY ue.user_id HAVING SUM(ue.count) >= :minEventCountThreshold LIMIT 1;
    ''', nativeQuery = true)
    public Long countOfUsersUsingSkillAfterAchievement(@Param("skillRefId") Integer skillRefId, @Param("minEventCountThreshold") Integer minEventCountThreshold)

    @Query(value='''
    SELECT COUNT(counts.user_id) AS count, counts.countBucket AS label 
        FROM 
        (
            SELECT ue.user_id as user_id, 
            CASE 
                WHEN SUM(ue.count) < 5 THEN '<5' 
                WHEN SUM(ue.count) >= 5 AND SUM(ue.count) < 20 THEN '>=5 <20' 
                WHEN SUM(ue.count) >= 20 AND SUM(ue.count) < 50 THEN '>=20 <50' 
                WHEN SUM(ue.count) >= 50 THEN '>=50' 
            END AS countBucket 
            FROM user_events ue, user_achievement achievements 
            WHERE 
                achievements.skill_ref_id = :skillRefId
                AND ue.skill_ref_id = :skillRefId 
                AND ue.user_id = achievements.user_id
                AND ue.event_time > achievements.achieved_on 
            GROUP BY ue.user_id
        )  AS counts GROUP BY counts.countBucket;
    ''', nativeQuery = true)
    public List<LabeledCount> binnedUserCountsForSkillUsagePostAchievement(@Param("skillRefId") Integer skillRefId)

    @Nullable
    @Query(value = '''
        SELECT max(event_time) FROM user_events where project_id = ?1
    ''', nativeQuery = true)
    Date getLatestEventDateForProject(String projectId)
}
