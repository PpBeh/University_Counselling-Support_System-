// Module 5 | Owner: Michael
// RLS makes this repository return published rows to students and all rows to
// counsellors/admins. All validation is repeated on the server.
package com.example.counselling.feature.resource.data

import com.example.counselling.core.ErrorMapper
import com.example.counselling.feature.resource.model.ResourceInsert
import com.example.counselling.feature.resource.model.ResourceRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class ResourceRepository(private val supabase: SupabaseClient) {

    suspend fun getResources(): Result<List<ResourceRow>> = try {
        val resources = supabase
            .from("resources")
            .select { order("title", Order.ASCENDING) }
            .decodeList<ResourceRow>()
        Result.success(resources)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load resources.")))
    }

    suspend fun createResource(input: ResourceInsert): Result<Unit> = try {
        supabase.from("resources").insert(input)
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not create the resource.")))
    }

    suspend fun updateResource(resourceId: String, input: ResourceInsert): Result<Unit> = try {
        supabase.from("resources").update(
            {
                set("title", input.title)
                set("description", input.description)
                set("category", input.category)
                set("media_type", input.mediaType)
                set("content_url", input.contentUrl)
                set("target_stress_brackets", input.targetStressBrackets)
                set("is_published", input.isPublished)
            },
        ) {
            filter { eq("id", resourceId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not update the resource.")))
    }

    suspend fun setPublished(resourceId: String, published: Boolean): Result<Unit> = try {
        supabase.from("resources").update(
            { set("is_published", published) },
        ) {
            filter { eq("id", resourceId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not change the publish state.")))
    }

    suspend fun deleteResource(resourceId: String): Result<Unit> = try {
        supabase.from("resources").delete {
            filter { eq("id", resourceId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not delete the resource.")))
    }
}
