$(document).ready(function() 
{ 
	//Open and close the tree
	lQuery('.emtree-widget ul li div .arrow').livequery('click', function(event){
			event.stopPropagation();
			var tree = $(this).closest(".emtree");
			var node = $(this).closest('.noderow');
			var nodeid = node.data('nodeid');
			var depth = node.data('depth');
			$('.emtree ul li div').removeClass('selected');
			
			var home = $(this).closest(".emtree").data("home");
			
			if ( $(this).find('.arrow').hasClass('down') )
			{
				$(this).find('.arrow').removeClass('down');				
			}
			else 
			{ 
				//Open it. add a UL
				$(this).find('.arrow').addClass('down');				
			}
			
			tree.find(nodeid + "_add").remove();
			node.load(home + "/components/emtree/tree.html?toggle=true&tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" + depth, 
				function()
				{
					$(window).trigger( "resize" ) 
				});
	});

	//Select a node
	lQuery('.emtree-widget ul li div .name').livequery('click', function(event) 
	{
		event.stopPropagation();
		$('.emtree ul li div').removeClass('selected');
		var tree = $(this).closest(".emtree");
		var node = $(this).closest('.noderow');
		$("div:first",node).addClass('selected');
		var nodeid = node.data('nodeid');	
		
		var prefix = tree.data("url-prefix");
		var targetdiv = tree.data("targetdiv");
		if(targetdiv ==  undefined || targetdiv == "" )
		{
			targetdiv = "searchlayout";
		}
		if( prefix)
		{
			var postfix = tree.data("url-postfix");
			gotopage(tree,node,prefix, postfix);
		}
		else
		{
			var home = tree.data("home");
			tree.find(nodeid + "_add").remove();
			var depth = node.data('depth');	
			//Not really needed?
			//node.load(home + "/components/emtree/tree.html?toggle=true&tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" + depth);
		}
		var event = $.Event( "emtreeselect" );
		event.tree = tree;
		event.nodeid = nodeid;
		$(document).trigger(event);
	});
	
	gotopage = function(tree, node, prefix, postfix)
	{

		var params = $(tree).closest(".treeclickparameters").data();
		if( params == undefined )
		{
			params = new Array();
		}

		var treeholder = $("div#categoriescontent");
		var toplocation =  parseInt( treeholder.scrollTop() );
		var leftlocation =  parseInt( treeholder.scrollLeft() );
		var targetdiv = tree.data("targetdiv");
		if(targetdiv ==  undefined || targetdiv == "" )
		{
			targetdiv = "searchlayout";
			params['oemaxlevel'] = 3;
		}
		
		var nodeid = node.data('nodeid');
		if( postfix == undefined || postfix == "" )
		{
			//postfix = ".html";
			postfix = "";
			prefix = prefix + "?categoryid=";
		}
		var home = tree.data("home");
		
		var depth = node.data('depth');
		
		params['tree-name'] = tree.data("treename");
		params['nodeID'] = nodeid;
		params['treetoplocation'] = toplocation;
		params['treeleftlocation'] = leftlocation;
		params['depth'] = depth;
		
		$.get(prefix + nodeid + postfix,
				params,	
				function(data) 
				{
					var cell = $("#" + targetdiv); //view-picker-content
					//console.log(cell);
					cell.replaceWith(data);
					//cell.html(data);
					//window.location.hash="TOP";
					$(window).trigger( "resize" );
				}
		);
	}
	
/*	
	$(".emtree-widget .delete").livequery('click', function(event) {
			event.stopPropagation();

			var id = $(this).data('parent');
			
			var agree=confirm("Are you sure you want to delete?");
			if (agree)
			{
				var tree = $(this).closest(".emtree");
				var home = tree.data("home");

				$.get(home + "/components/emtree/deletecategory.html", {
					categoryid: id,
					'tree-name': tree.data("treename"),
					} ,function () {
						tree.find("#" + id + "_row").hide( 'fast', function(){
							repaintEmTree(tree); 
						} );
						
					});
			} else {
				return false;
			}
	} );
*/			
	//need to init this with the tree
	lQuery("div#treeholder").livequery( function()
	{	
		var treeholder = $(this);
		var top = treeholder.data("treetoplocation");
		if( top )
		{
			var left = treeholder.data("treeleftlocation");
			var catcontent = $("div#categoriescontent");
			catcontent.scrollTop(parseInt(top));
			catcontent.scrollLeft(parseInt(left));
		}
	});

	lQuery('#treeholder input').livequery('click', function(event)
	{
		event.stopPropagation();
	});

	lQuery("#treeholder input").livequery('keyup', function(event) 
	{
       	var input = $(this);
       	var node = input.closest(".noderow");
       	var tree = input.closest(".emtree");
       	var value = input.val();
       	console.log("childnode",node);
       	var nodeid = node.data('nodeid');
		if( event.keyCode == 13 ) 
	  	{
	       	//13 represents Enter key
			var link = tree.data("home") + "/components/emtree/savenode.html?tree-name=" + tree.data("treename") + "&depth=" + node.data('depth');
			if(nodeid != undefined)
			{
				link = link + "&nodeID=" + nodeid;
				link = link + "&adding=true";
			}
			else
			{
				node = node.parent(".noderow");
				nodeid = node.data("nodeid");
				console.log("Dont want to save",node);
				if(nodeid != undefined)
				{
					link = link + "&parentNodeID=" + nodeid;
				}	
			}
			node.load(link, {edittext: value},function() 
			{
				//Reload tree in case it moved order
				repaintEmTree(tree);
			});
	  	}
		else if( event.keyCode === 27 ) //esc 
	  	{
			var link = tree.data("home") + "/components/emtree/tree.html?tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" + node.data('depth');
			link = link + "&adding=true";
			node.load(link); 	  		
	  	}
	});

	getNode = function(clickedon)
	{
		var clickedon = $(clickedon);
		var contextmenu = $(clickedon.closest(".treecontext"));
		var node = contextmenu.data("selectednoderow");
		contextmenu.hide();
		return node;
	}
	lQuery(".treecontext #nodeproperties").livequery('click', function(event) 
	{
				event.stopPropagation();
				var node = getNode(this);
				var tree = node.closest(".emtree");
				var nodeid = node.data('nodeid');
				var link = tree.data("home") + "/views/modules/category/edit/edit.html?categoryid=" + nodeid + "&id=" + nodeid + "&viewid=categorygeneral&viewpath=category/categorygeneral"; 
				document.location = link;
				return false;
	} );

	lQuery(".treecontext #addmedia").livequery('click', function(event) 
	{
				event.stopPropagation();
				var node = getNode(this);
				var tree = node.closest(".emtree");
				var nodeid = node.data('nodeid');
				
				//http://localhost:8080/assets/emshare/components/createmedia/upload/index.html?collectionid=AVgCmUw-cmJZ6_qmM-9u
				var url = tree.data("home") + "/components/createmedia/upload/index.html?";
				
				var collectionid = $("#collectiontoplevel").data("collectionid");
				var postfix = "";
				if( collectionid )
				{
					postfix = "&collectionid=" + collectionid;
					gotopage(tree,node,url,postfix);
				}
				else
				{
					url = tree.data("home") + "/views/upload/create/createnewstart.html?" + "nodeID=" + nodeid;
					document.location.href = url;
				}
				
						
				return false;
	} );

	
	lQuery(".treecontext #renamenode").livequery('click', function(event) {
				event.stopPropagation();
				var node = getNode(this);
				var tree = node.closest(".emtree");
				var nodeid = node.data('nodeid');
				var link = tree.data("home") + "/components/emtree/rename.html?tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" +  node.data('depth'); 
				node.find("> .categorydroparea").load(link , function()
				{
					node.find("input").select().focus();
				});
				return false;
	} );
	lQuery(".treecontext #deletenode").livequery('click', function(event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var nodeid = node.data('nodeid');
		var agree=confirm("Are you sure you want to delete?");
		if (agree)
		{
			console.log("removing",node, nodeid);
			var link = tree.data("home") + "/components/emtree/delete.html?tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" +  node.data('depth'); 
			$.get(link, function(data) 
			{
				repaintEmTree(tree);
			});
		}	
		return false;
	} );
	lQuery(".treecontext #createnode").livequery('click', function(event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var link = tree.data("home") + "/components/emtree/create.html?tree-name=" + tree.data("treename") + "&depth=" +  node.data('depth'); 
		$.get(link, function(data) {
		    node.append(data).fadeIn("slow");
			node.find("input").select().focus();
			$(document).trigger("domchanged");
		});
		return false;
	} );

	lQuery(".treecontext #createcollection").livequery('click', function(event) 
	{
		event.stopPropagation();
		var node = getNode(this);
		var nodeid = node.data('nodeid');
		var button = $("#collectionidaddbycategory");
		button.data("rootcategory",nodeid);
		button.data("name",node.data("categoryname"));
		
		button[0].click();
		return false;
	} );

	
function getPosition(e) {
  var posx = 0;
  var posy = 0;

  if (!e) var e = window.event;

  if (e.pageX || e.pageY) {
    posx = e.pageX;
    posy = e.pageY;
  } else if (e.clientX || e.clientY) {
    posx = e.clientX + document.body.scrollLeft + 
                       document.documentElement.scrollLeft;
    posy = e.clientY + document.body.scrollTop + 
                       document.documentElement.scrollTop;
  }

  return {
    x: posx,
    y: posy
  }
}
	
	
  $("body").on("contextmenu", ".noderow", function(e) 
  {
  	var noderow = $(this); // LI is the think that has context .find("> .noderow");
  	var pos = getPosition(e);
	var xPos = pos.x;
	var yPos = pos.y - 10;
	noderow.find("> .categorydroparea").addClass('selected'); //Keep it highlighted
	var emtreediv = noderow.closest(".emtree");
	
	
	var treename = emtreediv.data("treename"); 
	var $contextMenu = $( "#" + treename + "contextMenu");
	if( $contextMenu.length > 0 )
	{
		$contextMenu.data("selectednoderow",noderow);
		$contextMenu.css({
		  display: "block",
		      left: xPos,
		      top: yPos
		    });
		return false;
	}	
  });
		  
  lQuery('body').livequery('click', function () {
     	var $contextMenu = $(".treecontext");
     	$contextMenu.hide();
     	$(".categorydroparea:first").removeClass('selected');     	
    });

	//end document ready
	
});

repaintEmTree = function (tree) {
	var home = tree.data("home");
	
//	<div id="${treename}tree" class="emtree emtree-widget" data-home="$home$apphome" data-treename="$treename" data-rootnodeid="$rootcategory.getId()"
//		data-editable="$editable" data-url-prefix="$!prefix" data-url-postfix="$!postfix" data-targetdiv="$!targetdiv"
//		>
	var options = { 
			"tree-name": tree.data("treename"),
			"url-prefix":tree.data("url-prefix"),
			"url-postfix":tree.data("url-postfix"),
			"targetdiv":tree.data("targetdiv")
		};
	
	tree.closest("#treeholder").load(home +  "/components/emtree/tree.html", options, function()
	{
		$(document).trigger("domchanged");

	});
}