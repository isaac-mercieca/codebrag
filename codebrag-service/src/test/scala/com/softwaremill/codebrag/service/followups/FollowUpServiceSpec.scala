package com.softwaremill.codebrag.service.followups

import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.matchers.ShouldMatchers
import com.softwaremill.codebrag.dao._
import pl.softwaremill.common.util.time.FixtureTimeClock
import com.softwaremill.codebrag.domain.{FollowUp, CommitComment, CommitReview}
import scala.Some
import org.joda.time.DateTime
import org.bson.types.ObjectId

class FollowUpServiceSpec extends FlatSpec with MockitoSugar with ShouldMatchers with BeforeAndAfterEach with FollowUpServiceSpecFixture{

  var followUpDAO: FollowUpDAO = _
  var commitInfoDAO: CommitInfoDAO = _
  var commitReviewDAO: CommitReviewDAO = _

  var followUpsService: FollowUpService = _

  override def beforeEach() {
    followUpDAO = mock[FollowUpDAO]
    commitInfoDAO = mock[CommitInfoDAO]
    commitReviewDAO = mock[CommitReviewDAO]
    followUpsService = new FollowUpService(followUpDAO, commitInfoDAO, commitReviewDAO)(TestClock)
  }

  it should "generate follow-ups for commit for all commenters involved" in {
    // Given
    given(commitInfoDAO.findByCommitId(Commit.id)).willReturn(Some(Commit))
    given(commitReviewDAO.findById(Commit.id)).willReturn(Some(CommitReviewWithTwoComments))

    // When
    followUpsService.generateFollowUpsForCommit(Commit.id)

    // Then
    verify(followUpDAO).createOrUpdateExisting(FollowUp(Commit, UserOneId, FollowUpCreationDateTime))
    verify(followUpDAO).createOrUpdateExisting(FollowUp(Commit, UserTwoId, FollowUpCreationDateTime))
  }

  it should "generate follow-ups for each commenter only once" in {
    // Given
    given(commitInfoDAO.findByCommitId(Commit.id)).willReturn(Some(Commit))
    given(commitReviewDAO.findById(Commit.id)).willReturn(Some(CommitReviewWithNonUniqueCommenters))

    // When
    followUpsService.generateFollowUpsForCommit(Commit.id)

    // Then
    verify(followUpDAO).createOrUpdateExisting(FollowUp(Commit, UserOneId, FollowUpCreationDateTime))
    verify(followUpDAO).createOrUpdateExisting(FollowUp(Commit, UserTwoId, FollowUpCreationDateTime))
    verifyNoMoreInteractions(followUpDAO);
  }

  it should "throw exception and not generate follow-ups when commit not found" in {
    // Given
    given(commitInfoDAO.findByCommitId(Commit.id)).willReturn(None)
    given(commitReviewDAO.findById(Commit.id)).willReturn(Some(CommitReviewWithTwoComments))

    // When
    val thrown = intercept[RuntimeException] {
      followUpsService.generateFollowUpsForCommit(Commit.id)
    }
    thrown.getMessage should be(s"Commit ${Commit.id} not found. Cannot createOrUpdateExisting follow-ups for nonexisting commit")
    verifyZeroInteractions(followUpDAO)
  }

  it should "throw exception and not generate follow-ups for comments when no comments found" in {
    // Given
    given(commitInfoDAO.findByCommitId(Commit.id)).willReturn(Some(Commit))
    given(commitReviewDAO.findById(Commit.id)).willReturn(None)

    // When
    val thrown = intercept[RuntimeException] {
      followUpsService.generateFollowUpsForCommit(Commit.id)
    }
    thrown.getMessage should be(s"Commit review for commit ${Commit.id} not found. Cannot createOrUpdateExisting follow-ups for commit without comments")
    verifyZeroInteractions(followUpDAO)
  }

}

trait FollowUpServiceSpecFixture {

  val CommentDateTime = new DateTime()

  implicit val TestClock = new FixtureTimeClock(12345)
  val FollowUpCreationDateTime = TestClock.currentDateTimeUTC()

  val UserOneId = ObjectIdTestUtils.oid(456)
  val UserTwoId = ObjectIdTestUtils.oid(789)

  val Commit = CommitInfoBuilder.createRandomCommit()

  val UserOneComment = CommitComment(new ObjectId(), UserOneId, "user one comment", CommentDateTime)
  val UserTwoComment = CommitComment(new ObjectId(), UserTwoId, "user two comment", CommentDateTime)
  val UserTwoAnotherComment = CommitComment(new ObjectId(), UserTwoId, "user two another comment", CommentDateTime)

  val CommitReviewWithTwoComments = CommitReview(Commit.id, List(UserOneComment, UserTwoComment))
  val CommitReviewWithNonUniqueCommenters = CommitReview(Commit.id, List(UserOneComment, UserTwoComment, UserTwoAnotherComment))

}