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
package other

import scala.collection.JavaConverters._

import org.spongepowered.api.command.args.{CommandContext, GenericArguments}
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.entity.living.player.{Player, User}

import io.github.katrix.homesweethome.home.HomeHandler
import io.github.katrix.homesweethome.lib.{LibCommandKey, LibPerm}
import io.github.katrix.homesweethome.persistant.HomeConfig
import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.command.CommandBase
import io.github.katrix.katlib.helper.Implicits._
import io.github.katrix.katlib.lib.LibCommonCommandKey

class CmdHomeOtherInvite(homeHandler: HomeHandler, parent: CmdHomeOther)(implicit plugin: KatPlugin, config: HomeConfig) extends CommandBase(Some(parent)) {

	override def execute(src: CommandSource, args: CommandContext): CommandResult = {
		val data = for {
			player <- src.asInstanceOfOpt[Player].toRight(nonPlayerError).right
			homeOwner <- args.getOne[User]("homeOwner".text).toOption.toRight(playerNotFoundError).right
			target <- args.getOne[Player](LibCommonCommandKey.Player).toOption.toRight(playerNotFoundError).right
			homeName <- args.getOne[String](LibCommandKey.Home).toOption.toRight(invalidParameterError).right
			home <- homeHandler.specificHome(homeOwner.getUniqueId, homeName).toRight(homeNotFoundError).right
		} yield (player, homeOwner, target, homeName, home)

		data match {
			case Right((player, homeOwner, target, homeName, home)) =>
				homeHandler.addInvite(target, homeOwner.getUniqueId, home)
				src.sendMessage(config.text.inviteOtherSrc.value(Map(config.Target -> target.getName.text, config.HomeName -> homeName.text,
					config.Owner -> homeOwner.getName.text).asJava).build())
				target.sendMessage(config.text.inviteOtherPlayer.value(Map(config.HomeName -> homeName.text, config.Owner -> homeOwner.getName.text,
					config.Target -> player.getName.text).asJava).build())
				CommandResult.success()
			case Left(error) => throw error
		}
	}

	override def commandSpec: CommandSpec = CommandSpec.builder()
		.arguments(
			GenericArguments.user("homeOwner".text),
			GenericArguments.player(LibCommonCommandKey.Player),
			GenericArguments.remainingJoinedStrings(LibCommandKey.Home))
		.description("Invite someone else to another player's home".text)
		.permission(LibPerm.HomeOtherInvite)
		.executor(this)
		.build()

	override def aliases: Seq[String] = Seq("invite")
}