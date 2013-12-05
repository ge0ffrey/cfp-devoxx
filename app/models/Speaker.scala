/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Association du Paris Java User Group.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package models

import play.api.libs.json.Json
import library.{ZapJson, Redis}
import play.api.libs.Crypto

/**
 * Speaker
 *
 * Author: nicolas
 * Created: 28/09/2013 11:01
 */
case class Speaker(uuid:String, email: String, name:Option[String], bio: String, lang: Option[String], twitter: Option[String], avatarUrl: Option[String],
                   company: Option[String], blog: Option[String])

object Speaker {
  implicit val speakerFormat = Json.format[Speaker]

  def createSpeaker(email: String, name:String, bio: String, lang: Option[String], twitter: Option[String],
                    avatarUrl: Option[String], company: Option[String], blog: Option[String]): Speaker = {
    Speaker(Crypto.sign(email.trim().toLowerCase), email.trim().toLowerCase, Option(name), bio, lang, twitter, avatarUrl, company, blog)
  }

  def unapplyForm(s: Speaker): Option[(String, String, String, Option[String], Option[String], Option[String], Option[String], Option[String])] = {
    Some(s.email, s.name.getOrElse(""), s.bio, s.lang, s.twitter, s.avatarUrl, s.company, s.blog)
  }

  def save(speaker: Speaker) = Redis.pool.withClient {
    client =>
      val jsonSpeaker = Json.stringify(Json.toJson(speaker))
      client.hset("Speaker", speaker.uuid, jsonSpeaker)
  }

  def update(uuid: String, speaker: Speaker) = Redis.pool.withClient {
    client =>
      val jsonSpeaker = Json.stringify(Json.toJson(speaker.copy(uuid=uuid)))
      client.hset("Speaker", uuid, jsonSpeaker)
  }

  def updateName(uuid:String, newName:String) = {
    findByUUID(uuid).map{ speaker=>
      Speaker.update(uuid, speaker.copy(name=Option(newName)))
    }
  }

  def findByUUID(uuid: String): Option[Speaker] = Redis.pool.withClient {
    client =>
      client.hget("Speaker", uuid).flatMap {
        json: String =>
          Json.parse(json).validate[Speaker].fold(invalid=>{ play.Logger.error("Speaker error. "+ZapJson.showError(invalid));None},validSpeaker=>Some(validSpeaker))
      }
  }

  def delete(uuid: String) = Redis.pool.withClient {
    client =>
      client.hdel("Speaker", uuid)
  }

  def allSpeakers(): List[Speaker] = Redis.pool.withClient {
    client =>
      client.hvals("Speaker").flatMap {
        jsString =>
          val maybeSpeaker = Json.parse(jsString).asOpt[Speaker]
          maybeSpeaker
      }
  }

  def countAll():Long = Redis.pool.withClient{
    client=>
      client.hlen("Speaker")
  }


}

