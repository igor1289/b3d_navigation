;	NAVIGATION

Type NAV_Graph
	Field FirstNode.NAV_Node
	Field LastNode.NAV_Node
End Type

Type NAV_Node
	Field Pivot
	Field Disabled
	Field ParentDist
	Field Parent.NAV_Node			; for path building
	Field F#, G#, H#
	
	Field PrevNode.NAV_Node
	Field NextNode.NAV_Node
	
	Field FirstLink.NAV_Link
	Field LastLink.NAV_Link
End Type

Type NAV_Link
	Field Node.NAV_Node
	Field Distance#
	
	Field PrevLink.NAV_Link
	Field NextLink.NAV_Link
End Type

Type NAV_OpenedNode
	Field Node.NAV_Node
End Type

Type NAV_ClosedNode
	Field Node.NAV_Node
End Type

Type NAV_Path
	Field Cost#, Nodes
	Field StartNode.NAV_PathNode
End Type

Type NAV_PathNode
	Field Node.NAV_Node
	
	Field NextNode.NAV_PathNode
End Type

Function NAV_AddNode.NAV_Node(Graph.NAV_Graph, Node.NAV_Node)
	If Graph\LastNode<>Null
		Node\PrevNode=Graph\LastNode
		Graph\LastNode\NextNode=Node
		Graph\LastNode=Node
	Else
		Graph\FirstNode=Node
		Graph\LastNode=Node
	EndIf
	
	Return Node
End Function

Function NAV_AddLink(NodeA.NAV_Node, NodeB.NAV_Node)
	Local Link.NAV_Link=New NAV_Link
	Link\Distance=EntityDistance(NodeA\Pivot, NodeB\Pivot)
	Link\Node=NodeB
	
	If NodeA\LastLink<>Null
		Link\PrevLink=NodeA\LastLink
		NodeA\LastLink\NextLink=Link
		NodeA\LastLink=Link
	Else
		NodeA\FirstLink=Link
		NodeA\LastLink=Link
	EndIf
End Function

Function NAV_Connect(NodeA.NAV_Node, NodeB.NAV_Node)
	NAV_AddLink(NodeA, NodeB)
	NAV_AddLink(NodeB, NodeA)
	
	;c=CreateCube(NodeA\Pivot)
	;ScaleEntity c, .1, .1, EntityDistance(NodeA\Pivot, NodeB\Pivot)/2
	;PointEntity c, NodeB\Pivot
	;MoveEntity c, 0, 0, EntityDistance(NodeA\Pivot, NodeB\Pivot)/2
End Function

Function NAV_Heuristic#(Node.NAV_Node, DstNode.NAV_Node)
	Local X#=Abs(EntityX(DstNode\Pivot, True)-EntityX(Node\Pivot, True))
	Local Y#=Abs(EntityY(DstNode\Pivot, True)-EntityY(Node\Pivot, True))
	Local Z#=Abs(EntityZ(DstNode\Pivot, True)-EntityZ(Node\Pivot, True))
	
	Return (X+Y+Z)
End Function

Function NAV_AddToOpened(Node.NAV_Node)
	Local Opened.NAV_OpenedNode
	Local L.NAV_OpenedNode
	Local R.NAV_OpenedNode
	
	L=First NAV_OpenedNode
	
	;Если список открытых узлов не пуст
	If L<>Null
		;Если F открываемого узла меньше F первого узла в списке то добавляем открываемый узел в начало списка
		If Node\F<=L\Node\F
			Opened=New NAV_OpenedNode
			Opened\Node=Node
			Insert Opened Before L
			Return
		EndIf
	Else
		;Создаём первый узел в списке если список пуст
		Opened=New NAV_OpenedNode
		Opened\Node=Node
		Return
	EndIf

	;Во всех остальных случаях вставляем узел в список открытых, с сортировкой
	Repeat
		R=After L
		If R=Null Then Exit
		
		If Node\F>=L\Node\F And Node\F<=R\Node\F
			Opened=New NAV_OpenedNode
			Opened\Node=Node
			Insert Opened Before R
			Return
		EndIf
		L=R
	Forever
	
	;Вставляем узел в самый конец списка
	Opened=New NAV_OpenedNode
	Opened\Node=Node
	
End Function

Function NAV_AddToClosed(Node.NAV_Node)
	Local Closed.NAV_ClosedNode
	Local L.NAV_ClosedNode
	Local R.NAV_ClosedNode
	
	;Получаем первый узел в списке закрытых
	L=First NAV_ClosedNode
	
	;Если список закрытых узлов не пуст
	If L<>Null
		;Если F закрываемого узла меньше F первого узла в списке то добавляем открываемый узел в начало списка
		If Node\F<=L\Node\F
			Closed=New NAV_ClosedNode
			Closed\Node=Node
			Insert Closed Before L
			Return
		EndIf
	Else
		;Создаём первый узел в списке если список пуст
		Closed=New NAV_ClosedNode
		Closed\Node=Node
		Return
	EndIf
	
	;Во всех остальных случаях вставляем узел в список закрытых, с сортировкой
	Repeat
		R=After L
		If R=Null Then Exit
		
		If Node\F>=L\Node\F And Node\F<=R\Node\F
			Closed=New NAV_ClosedNode
			Closed\Node=Node
			Insert Closed Before R
			Return
		EndIf
		L=R
	Forever
	
	;Вставляем узел в самый конец списка
	Closed=New NAV_ClosedNode
	Closed\Node=Node
End Function

Function NAV_SortNode(Opened.NAV_OpenedNode)

	;Функция работает точно так же как и NAV_AddToOpened, только удаляет открытый узел и заново его вставляет
	Local Node.NAV_Node=Opened\Node
	Delete Opened
	
	Local L.NAV_OpenedNode
	Local R.NAV_OpenedNode
	
	L=First NAV_OpenedNode
	
	If L<>Null
		If Node\F<=L\Node\F
			Opened=New NAV_OpenedNode
			Opened\Node=Node
			Insert Opened Before L
			Return
		EndIf
	Else
		Opened=New NAV_OpenedNode
		Opened\Node=Node
		Return
	EndIf
		
	Repeat
		R=After L
		If R=Null Then Exit
			
		If Node\F>=L\Node\F And Node\F<=R\Node\F
			Opened=New NAV_OpenedNode
			Opened\Node=Node
			Insert Opened Before R
			Return
		EndIf
		L=R
	Forever
	
	Opened=New NAV_OpenedNode
	Opened\Node=Node
	
End Function

Function NAV_IsOpened.NAV_OpenedNode(Node.NAV_Node)
	For Opened.NAV_OpenedNode=Each NAV_OpenedNode
		If Opened\Node=Node Then Return Opened
		
		;Поскольку все узлы вставляются с сортировкой по F в порядке возрастания, то при нахождении узла с бОльшим F можно прерывать поиск
		If Opened\Node\F>Node\F Then Return Null
	Next
End Function

Function NAV_IsClosed(Node.NAV_Node)
	For Closed.NAV_ClosedNode=Each NAV_ClosedNode
		If Closed\Node=Node Then Return True

		;Поскольку все узлы вставляются с сортировкой по F в порядке возрастания, то при нахождении узла с бОльшим F можно прерывать поиск
		If Closed\Node\F>Node\F Then Return
	Next
End Function

Function NAV_FindPath.NAV_Path(SrcNode.NAV_Node, DstNode.NAV_Node, ExcludeSrcNode=True)
	Local Node.NAV_Node
	Local Link.NAV_Link
	Local LinkNode.NAV_Node
	Local Opened.NAV_OpenedNode
	Local Closed.NAV_ClosedNode
	
	;Обнуляем дистанцию до исходного узла
	SrcNode\ParentDist=0
	
	;Добавляем исходный узел в список открытых узлов
	NAV_AddToOpened(SrcNode)
	
	Local Fail
	
	Repeat
		;Берём первый узел из списка открытых узлов
		Opened=First NAV_OpenedNode
		
		;Если список пуст то поиск закончился неудачей
		If Opened=Null
			Fail=True
			Exit
		EndIf
		
		Node=Opened\Node
		Link=Node\FirstLink
		
		;Удаляем открытый узел из списка открытых и помещаем его в список закрытых
		Delete Opened
		NAV_AddToClosed(Node)
		
		;Обрабатываем ссылки на соединенные соседние узлы
		While Link<>Null
			
			LinkNode=Link\Node
			If LinkNode\Disabled=False
				
				;Если узел не находится в списке открытых узлов
				If NAV_IsClosed(LinkNode)=False

					;Подсчитываем сумму G для узла. G - "стоимость" перехода на узел. По умолчанию - это дистанция
					Local G=Node\G+Link\Distance
					
					;Если соседний узел - это конечный узел, то прекращаем поиск и переходим к построению пути
					If LinkNode=DstNode
						LinkNode\Parent=Node ;Устанавливаем родительским узлом текущий узел
						LinkNode\ParentDist=Link\Distance ;Устанавливаем дистанцию до родительского узла из ссылки
						Exit
					EndIf
					
					;Проверяем находится ли соседний узел в списке открытых и, если узел найден - получаем запись списка открытых узлов
					Local OpenedLinkNode.NAV_OpenedNode=NAV_IsOpened(LinkNode)
					
					;Если узел не находится в списке открытых
					If OpenedLinkNode=Null
						LinkNode\Parent=Node	;Устанавливаем родительским узлом текущий узел
						LinkNode\ParentDist=Link\Distance ;Устанавливаем дистанцию до родительского узла из ссылки
						LinkNode\G=G ;Устанавливаем сумму G для узла на рассчитанную выше
						LinkNode\H=NAV_Heuristic(LinkNode, DstNode) ;Рассчитываем эвристику для соседнего узла
						LinkNode\F=LinkNode\G+LinkNode\H ;Рассчитываем сумму F для узла как сумму G и эвристику (H)
						NAV_AddToOpened(LinkNode) ; Добавляем узел в список открытых узлов
					Else
						;Если стоимость G текущего узла меньше стоимости G соседнего узла
						If G<LinkNode\G
							LinkNode\Parent=Node	;Устанавливаем родителя соседнего узла как текущий узел...
							LinkNode\ParentDist=Link\Distance ;...А так же расстояние
							LinkNode\G=G 			;Стоимость G устанавливаем как для текущего узла
							LinkNode\F=LinkNode\G+LinkNode\H ;Перерасчитываем сумму F для соседнего узла
							NAV_SortNode(OpenedLinkNode) ; Сортируем узел в списке открытых узлов
						EndIf
					EndIf
				EndIf
			EndIf
			
			Link=Link\NextLink
		Wend
		
		;Если последний узел - конечный узел, то прекращаем поиск и строим путь
		If LinkNode=DstNode
			LinkNode\ParentDist=Link\Distance
			Exit
		EndIf
	Forever
	
	If Fail=False
		Local Parent.NAV_Node=DstNode
		
		Local PathNode.NAV_PathNode=New NAV_PathNode
		PathNode\Node=Parent
		
		Local NextNode.NAV_PathNode=PathNode
		
		Local Cost#=Parent\ParentDist
		Local Nodes=1
		
		Repeat
			Parent=Parent\Parent
			Cost=Cost+Parent\ParentDist
			Nodes=Nodes+1
			
			If Parent=SrcNode
				If ExcludeSrcNode
					Exit
				Else
					PathNode=New NAV_PathNode
					PathNode\Node=Parent
					PathNode\NextNode=NextNode
					NextNode=PathNode
					Exit
				EndIf
			Else
				PathNode=New NAV_PathNode
				PathNode\Node=Parent
				PathNode\NextNode=NextNode
				NextNode=PathNode
			EndIf
		Forever
		
		Local Path.NAV_Path=New NAV_Path
		Path\Cost=Cost
		Path\Nodes=Nodes
		Path\StartNode=PathNode
		
		Delete Each NAV_OpenedNode
		Delete Each NAV_ClosedNode
		
		Return Path								; Finally return path
	Else
		Delete Each NAV_OpenedNode				; ...Or null
		Delete Each NAV_ClosedNode
	EndIf
End Function

Function NAV_FindNode.NAV_Node(Graph.NAV_Graph, Name$)
	Local Node.NAV_Node=Graph\FirstNode
	
	While Node<>Null
		If EntityName(Node\Pivot)=Name$ Then Return Node
		Node=Node\NextNode
	Wend
	
	Return Node
End Function

Function NAV_NearestNode.NAV_Node(Graph.NAV_Graph, Entity)
	Local MinDistance#=10^38
	Local NearestNode.NAV_Node
	Local Node.NAV_Node=Graph\FirstNode
	
	While Node<>Null
		If Node\Disabled=False
			Local Distance#=EntityDistance(Entity, Node\Pivot)
			If Distance<MinDistance
				MinDistance=Distance
				NearestNode=Node
			EndIf
		EndIf
		
		Node=Node\NextNode
	Wend
	
	Return NearestNode
End Function

Function NAV_DeletePath(Path.NAV_Path)
	Local PathNode.NAV_PathNode=Path\StartNode
	Local NextPathNode.NAV_PathNode
	
	While PathNode<>Null
		NextPathNode=PathNode\NextNode
		Delete PathNode
		PathNode=NextPathNode
	Wend
	
	Delete Path
End Function

Function NAV_DeleteGraph(Graph.NAV_Graph)
	Local Node.NAV_Node=Graph\FirstNode
	
	While Node<>Null
		Local NextNode.NAV_Node=Node\NextNode
		Delete Node
		Node=NextNode
	Wend
	
	Delete Graph
End Function

Function NAV_Clear()
	Delete Each NAV_Graph
	Delete Each NAV_Node
	Delete Each NAV_Link
	Delete Each NAV_Path
	Delete Each NAV_PathNode
End Function

;	GRAPH LOADER

Dim NAV_LOADER_GRAPH.NAV_Node(0)

Function NAV_LoadGraph.NAV_Graph(Path$, Entity)
	Local Error

	Local File=ReadFile(Path)
	
	If File=0
		Return Null
	EndIf
	
	;	Load nodes
	
	Local Nodes=ReadByte(File)
	
	Dim NAV_LOADER_GRAPH(Nodes)
	Nodes=Nodes-1
	
	For Index=0 To Nodes
		Local Name$=ReadString(File)
		Local Child=FindChild(Entity, Name)
		
		If Child=0
			Error=True
			Exit
		EndIf
		
		Local Node.NAV_Node=New NAV_Node
		Node\Pivot=Child
		NAV_LOADER_GRAPH(Index)=Node
	Next
	
	;	Terminate graph if error occured
	
	If Error
	
		For Index=0 To Nodes
			If NAV_LOADER_GRAPH(Index)<>Null
				Delete NAV_LOADER_GRAPH(Index)
			Else
				Return
			EndIf
		Next
	EndIf
	
	;	Link nodes
	
	Local Links=ReadByte(File)-1
	
	For Index=0 To Links
		Local NodeA=ReadByte(File)
		Local NodeB=ReadByte(File)
		
		NAV_Connect(NAV_LOADER_GRAPH(NodeA), NAV_LOADER_GRAPH(NodeB))
	Next
	
	CloseFile(File)
	
	Local Graph.NAV_Graph=New NAV_Graph
	
	For Index=0 To Nodes
		NAV_AddNode(Graph, NAV_LOADER_GRAPH(Index))
	Next
	
	Return Graph
End Function