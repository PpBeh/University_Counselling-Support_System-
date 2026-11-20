// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.domain

// OOP: Abstraction - resource and self-check ViewModels depend on this contract.
interface ResourceRepository {

    suspend fun getResources(): Result<List<Resource>>

    suspend fun getResource(resourceId: String): Result<Resource?>

    suspend fun createResource(draft: ResourceDraft): Result<Resource>

    suspend fun updateResource(resourceId: String, draft: ResourceDraft): Result<Resource>

    suspend fun setPublished(resourceId: String, published: Boolean): Result<Unit>

    suspend fun deleteResource(resourceId: String): Result<Unit>
}
