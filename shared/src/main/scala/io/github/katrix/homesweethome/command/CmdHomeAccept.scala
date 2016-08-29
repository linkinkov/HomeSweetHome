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

import scala.collection.JavaConverters._

import org.spongepowered.api.command.args.{CommandContext, GenericArguments}
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.command.{CommandException, CommandResult, CommandSource}
import org.spongepowered.api.entity.living.player.Player

import io.github.katrix.homesweethome.home.HomeHandler
import io.github.katrix.homesweethome.lib.LibPerm
import io.github.katrix.homesweethome.persistant.HomeConfig
import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.command.CommandBase
import io.github.katrix.katlib.helper.Implicits._
import io.github.katrix.katlib.lib.LibCommonCommandKey

class CmdHomeAccept(homeHandler: HomeHandler, parent: CmdHome)(implicit plugin: KatPlugin, config: HomeConfig) extends CommandBase(Some(parent)) {

	override def execute(src: CommandSource, args: CommandContext): CommandResult = {
		val data = for {
			player <- src.asInstanceOfOpt[Player].toRight(nonPlayerError).right
			requester <- args.getOne[Player](LibCommonCommandKey.Player).toOption.toRight(playerNotFoundError).right
			home <- homeHandler.getRequest(requester, player.getUniqueId).toRight(new CommandException(config.text.invalidRequest.value)).right
		} yield (player, requester, home)

		data match {
			case Right((homeOwner, requester, home)) if home.teleport(requester) =>
				requester.sendMessage(config.text.acceptRequester.value)
				src.sendMessage(config.text.acceptSuccess.value(Map(config.Requester -> requester.getName.text).asJava).build())
				homeHandler.removeRequest(requester, homeOwner.getUniqueId)
				CommandResult.success()
			case Right((player, requester, home)) => throw teleportError
			case Left(error) => throw error
		}
	}

	override def commandSpec: CommandSpec = CommandSpec.builder()
		.arguments(GenericArguments.player(LibCommonCommandKey.Player))
		.description("Accept a home request".text)
		.permission(LibPerm.HomeAccept)
		.executor(this)
		.build()

	override def aliases: Seq[String] = Seq("accept")
}
