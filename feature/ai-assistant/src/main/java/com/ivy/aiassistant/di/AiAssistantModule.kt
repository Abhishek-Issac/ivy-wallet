package com.ivy.aiassistant.di

import android.content.Context
import com.ivy.aiassistant.data.local.AiAssistantDatabase
import com.ivy.aiassistant.data.local.AiConversationDao
import com.ivy.aiassistant.data.local.AiMessageDao
import com.ivy.aiassistant.tools.ToolExecutor
import com.ivy.aiassistant.tools.builtin.AppInfoTool
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiAssistantDbModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AiAssistantDatabase =
        AiAssistantDatabase.create(context)

    @Provides
    fun provideConversationDao(db: AiAssistantDatabase): AiConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: AiAssistantDatabase): AiMessageDao = db.messageDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AiAssistantToolsModule {

    @Binds
    @IntoSet
    abstract fun bindAppInfoTool(impl: AppInfoTool): ToolExecutor
}
