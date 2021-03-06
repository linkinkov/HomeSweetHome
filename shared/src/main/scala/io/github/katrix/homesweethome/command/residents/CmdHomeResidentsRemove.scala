/*
 * This file is part of HomeSweetHome, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 Katrix
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.katrix.homesweethome.command
package residents

import java.util.Locale

import org.spongepowered.api.command.args.{CommandContext, GenericArguments}
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.command.{CommandException, CommandResult, CommandSource}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors._

import io.github.katrix.homesweethome.HSHResource
import io.github.katrix.homesweethome.home.{Home, HomeHandler}
import io.github.katrix.homesweethome.lib.{LibCommandKey, LibPerm}
import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.command.LocalizedCommand
import io.github.katrix.katlib.helper.Implicits._
import io.github.katrix.katlib.i18n.Localized
import io.github.katrix.katlib.lib.LibCommonCommandKey

class CmdHomeResidentsRemove(homeHandler: HomeHandler, parent: CmdHomeResidents)(implicit plugin: KatPlugin) extends LocalizedCommand(Some(parent)) {

  override def execute(src: CommandSource, args: CommandContext): CommandResult = Localized(src) { implicit locale =>
    val data = for {
      player <- playerTypeable.cast(src).toRight(nonPlayerErrorLocalized)
      target <- args.getOne[Player](LibCommonCommandKey.Player).toOption.toRight(playerNotFoundErrorLocalized)
      home   <- args.getOne[(Home, String)](LibCommandKey.Home).toOption.toRight(homeNotFoundError)
    } yield (player, target, home._1, home._2)

    data match {
      case Right((player, target, home, homeName)) if home.residents.contains(target.getUniqueId) =>
        val newHome = home.removeResident(target.getUniqueId)
        homeHandler.updateHome(player.getUniqueId, homeName, newHome)
        src.sendMessage(t"$GREEN${HSHResource.get("cmd.residentsRemove.playerSuccess", "target" -> target.getName, "homeName" -> homeName)}")
        target.sendMessage(t"$YELLOW${HSHResource.get("cmd.residentsRemove.targetSuccess", "homeName" -> homeName, "player" -> player.getName)}")
        CommandResult.success()
      case Right((_, target, _, homeName)) =>
        throw new CommandException(t"$RED${HSHResource.get("cmd.residentsRemove.notAResident", "target" -> target.getName, "homeName" -> homeName)}")
      case Left(error) => throw error
    }
  }

  override def localizedDescription(implicit locale: Locale): Option[Text] = Some(HSHResource.getText("cmd.residentsRemove.description"))

  override def commandSpec: CommandSpec =
    CommandSpec
      .builder()
      .arguments(GenericArguments.player(LibCommonCommandKey.Player), new CommandElementHome(LibCommandKey.Home, homeHandler))
      .description(this)
      .permission(LibPerm.HomeResidentsRemove)
      .executor(this)
      .build()

  override def aliases: Seq[String] = Seq("remove", "delete")
}
