/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vapingduty.connectors.helpers

import org.scalatest.prop.TableDrivenPropertyChecks
import uk.gov.hmrc.vapingduty.base.SpecBase

import java.time.LocalDate

class ObligationsDateRangeSpec extends SpecBase with TableDrivenPropertyChecks {

  private val yearsToLookBack = 3
  private val noMonthsAhead   = 0

  private def date(s: String): LocalDate = LocalDate.parse(s)

  private val scenarios = Table(
    ("description", "today", "expectedFrom", "expectedTo"),
    ("mid-month", "2024-06-11", "2021-06-30", "2024-05-01"),
    ("the 1st of January (toDate rolls back a year)", "2025-01-01", "2022-01-31", "2024-12-01"),
    ("the 31st of December", "2024-12-31", "2021-12-31", "2024-11-01"),
    ("a leap day, where 3 years ago is not a leap year", "2024-02-29", "2021-02-28", "2024-01-01"),
    ("the 28th of February with no leap years involved", "2025-02-28", "2022-02-28", "2025-01-01"),
    ("the 28th of February, where 3 years ago is a leap year", "2027-02-28", "2024-02-29", "2027-01-01"),
    ("the 1st of March, with toDate in a non-leap February", "2025-03-01", "2022-03-31", "2025-02-01"),
    ("the 31st of March, with toDate in a leap February", "2024-03-31", "2021-03-31", "2024-02-01"),
    ("the 1st of the month", "2026-10-01", "2023-10-31", "2026-09-01"),
    ("the last day of the month", "2026-10-31", "2023-10-31", "2026-09-01"),
    ("mid-October 2026 (October must be excluded)", "2026-10-15", "2023-10-31", "2026-09-01")
  )

  "ObligationsDateRange" - {
    forAll(scenarios) { (description, today, expectedFrom, expectedTo) =>
      s"when today is $description ($today)" - {
        val range = ObligationsDateRange(date(today), yearsToLookBack, noMonthsAhead)

        s"must return fromDate $expectedFrom and toDate $expectedTo" in {
          range mustBe ObligationsDateRange(date(expectedFrom), date(expectedTo))
        }

        "must not include any day of the current month" in {
          range.toDate.isBefore(date(today).withDayOfMonth(1)) mustBe true
        }

        "must start on the last day of a month and end on the first day of a month" in {
          range.fromDate.getDayOfMonth mustBe range.fromDate.lengthOfMonth
          range.toDate.getDayOfMonth mustBe 1
        }
      }
    }

    "must shift the range forward by the configured months ahead" in {
      ObligationsDateRange(date("2024-06-11"), yearsToLookBack, monthsAhead = 1) mustBe
        ObligationsDateRange(date("2021-07-31"), date("2024-06-01"))
    }

    "must look back by the configured number of years" in {
      ObligationsDateRange(date("2024-06-11"), yearsToLookBack = 1, noMonthsAhead) mustBe
        ObligationsDateRange(date("2023-06-30"), date("2024-05-01"))
    }
  }
}
