package com.example.data.curriculum

import com.example.domain.model.LessonPlanTemplateSection

/** Mirrors DEFAULT_LESSON_TEMPLATE in worker/src/index.js. */
object DefaultLessonPlanTemplate {
  const val NAME = "KICD CBC Lesson Plan"

  val sections: List<LessonPlanTemplateSection> = listOf(
    LessonPlanTemplateSection("sec1", "Specific Learning Outcomes"),
    LessonPlanTemplateSection("sec3", "Core Competencies"),
    LessonPlanTemplateSection("sec2", "Key Inquiry Question"),
    LessonPlanTemplateSection("sec6", "Learning Resources"),
    LessonPlanTemplateSection("sec13", "Organization of Learning"),
    LessonPlanTemplateSection("sec7", "Introduction"),
    LessonPlanTemplateSection("sec8", "Lesson Development"),
    LessonPlanTemplateSection("sec9", "Conclusion"),
    LessonPlanTemplateSection("sec10", "Extended Activity"),
    LessonPlanTemplateSection("sec12", "Reflection"),
  )
}
