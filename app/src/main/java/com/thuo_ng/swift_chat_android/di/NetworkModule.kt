package com.thuo_ng.swift_chat_android.di

import com.thuo_ng.swift_chat_android.BuildConfig
import com.thuo_ng.swift_chat_android.core.network.AuthInterceptor
import com.thuo_ng.swift_chat_android.core.network.TokenAuthenticator
import com.thuo_ng.swift_chat_android.data.remote.api.AuthApi
import com.thuo_ng.swift_chat_android.data.remote.api.ConversationApi
import com.thuo_ng.swift_chat_android.data.remote.api.MessageApi
import com.thuo_ng.swift_chat_android.data.remote.api.NotificationApi
import com.thuo_ng.swift_chat_android.data.remote.api.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    @Named("AuthClient")
    fun provideAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("AuthRetrofit")
    fun provideAuthRetrofit(
        @Named("AuthClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("AuthApi")
    fun provideAuthApi(
        @Named("AuthRetrofit") retrofit: Retrofit
    ): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    @Named("MainClient")
    fun provideMainOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("MainRetrofit")
    fun provideMainRetrofit(
        @Named("MainClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApi(
        @Named("MainRetrofit") retrofit: Retrofit
    ): UserApi {
        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideConversationApi(
        @Named("MainRetrofit") retrofit: Retrofit
    ): ConversationApi {
        return retrofit.create(ConversationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMessageApi(
        @Named("MainRetrofit") retrofit: Retrofit
    ): MessageApi {
        return retrofit.create(MessageApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApi(
        @Named("MainRetrofit") retrofit: Retrofit
    ): NotificationApi {
        return retrofit.create(NotificationApi::class.java)
    }
}
