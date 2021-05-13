package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class CommitElement(
    @SerializedName("commit")
    val commit: Commit
) {
    val lastCommitDate
        get() = commit.committer.date
}